package mousio.jetcd.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.requests.EtcdKeyRequest;
import mousio.jetcd.requests.EtcdRequest;
import mousio.jetcd.requests.EtcdVersionRequest;
import mousio.jetcd.responses.EtcdKeysResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Netty client for the requests and responses
 */
public class EtcdNettyClient implements EtcdClientImpl {
  private final Bootstrap bootstrap;
  private final NioEventLoopGroup eventLoopGroup;

  private final URI[] uris;
  protected int lastWorkingUriIndex = 0;

  /**
   * Constructor
   *
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uri        to connect to
   */
  public EtcdNettyClient(SslContext sslContext, URI... uri) {
    this.eventLoopGroup = new NioEventLoopGroup();

    this.uris = uri;

    // Configure the client.
    this.bootstrap = new Bootstrap();
    bootstrap.group(eventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
              p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast("codec", new HttpClientCodec());
            p.addLast("aggregate", new HttpObjectAggregator(1024 * 100));
          }
        });
  }

  /**
   * Send a request and get a future.
   *
   * @param etcdRequest Etcd Request to send
   * @return Promise for the request.
   */
  public <R> EtcdResponsePromise<R> send(EtcdRequest<R> etcdRequest) throws IOException {
    if (etcdRequest.getPromise() == null) {
      EtcdResponsePromise<R> responsePromise = new EtcdResponsePromise<>();
      etcdRequest.setPromise(responsePromise);
    }

    final ConnectionCounter counter = new ConnectionCounter();
    counter.uriIndex = lastWorkingUriIndex;

    connect(etcdRequest, counter);

    return etcdRequest.getPromise();
  }

  /**
   * Connect to server
   *
   * @param etcdRequest to request with
   * @param counter     for retries
   * @param <R>         Type of response
   * @throws IOException if request could not be sent.
   */
  protected <R> void connect(EtcdRequest<R> etcdRequest, ConnectionCounter counter) throws IOException {
    // Start the connection attempt.
    ChannelFuture connectFuture = bootstrap.clone()
        .connect(uris[counter.uriIndex].getHost(), uris[counter.uriIndex].getPort());

    Channel channel = connectFuture.channel();
    DefaultPromise<R> p = new DefaultPromise<>(connectFuture.channel().eventLoop());
    etcdRequest.getPromise().attachNettyPromise(p);

    connectFuture.awaitUninterruptibly();

    if (!connectFuture.isSuccess()) {
      counter.uriIndex++;
      if (counter.uriIndex >= uris.length) {
        if (counter.retryCount >= 3) {
          etcdRequest.getPromise().setException(connectFuture.cause());
          return;
        }
        counter.retryCount++;
        counter.uriIndex = 0;
      }

      connect(etcdRequest, counter);
      return;
    }

    lastWorkingUriIndex = counter.uriIndex;

    modifyPipeLine(etcdRequest, p, channel.pipeline());

    HttpRequest httpRequest = createHttpRequest(etcdRequest.getUri(), etcdRequest);

    // send request
    channel.writeAndFlush(httpRequest);
  }

  /**
   * Get HttpRequest belonging to etcdRequest
   *
   * @param uri         to send request to
   * @param etcdRequest to send
   * @return HttpRequest
   * @throws IOException if request could not be created
   */
  public static <R> HttpRequest createHttpRequest(String uri, EtcdRequest<R> etcdRequest) throws IOException {
    HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, etcdRequest.getMethod(), uri);
    try {
      httpRequest = setRequestParameters(etcdRequest, httpRequest);
    } catch (Exception e) {
      throw new IOException(e);
    }
    return httpRequest;
  }

  /**
   * Modify the pipeline for the request
   *
   * @param req             to process
   * @param responsePromise Promise for the response
   * @param pipeline        to modify
   * @param <R>             Type of Response
   */
  @SuppressWarnings("unchecked")
  private <R> void modifyPipeLine(EtcdRequest<R> req, Promise<R> responsePromise, ChannelPipeline pipeline) {
    if (req instanceof EtcdKeyRequest) {
      pipeline.addLast(new EtcdKeyResponseHandler((Promise<EtcdKeysResponse>) responsePromise));
    } else if (req instanceof EtcdVersionRequest) {
      pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
          ((Promise<String>) responsePromise)
              .setSuccess(
                  msg.content().toString(Charset.defaultCharset()));
        }
      });
    } else {
      throw new RuntimeException("Unknown request type " + req.getClass().getName());
    }
  }

  /**
   * Set parameters on request
   *
   * @param etcdRequest to send
   * @param httpRequest to send
   * @return Http Request
   * @throws Exception on fail
   */
  private static HttpRequest setRequestParameters(EtcdRequest<?> etcdRequest, HttpRequest httpRequest) throws Exception {
    // Set possible key value pairs
    Map<String, String> keyValuePairs = etcdRequest.getRequestParams();
    if (keyValuePairs != null && !keyValuePairs.isEmpty()) {
      if (etcdRequest.getMethod() == HttpMethod.POST) {
        HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(httpRequest, false);
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
          bodyRequestEncoder.addBodyAttribute(entry.getKey(), entry.getValue());
        }

        httpRequest = bodyRequestEncoder.finalizeRequest();
        bodyRequestEncoder.close();
      } else {
        String getLocation = "";
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
          if (!getLocation.isEmpty()) {
            getLocation += "&";
          }
          getLocation += entry.getKey() + "=" + entry.getValue();
        }

        httpRequest.setUri(etcdRequest.getUri().concat("?").concat(getLocation));
      }
    }
    return httpRequest;
  }

  /**
   * Close netty
   */
  public void close() {
    eventLoopGroup.shutdownGracefully();
  }

  /**
   * Counts connection retries and current connection index
   */
  protected class ConnectionCounter {
    public int uriIndex;
    public int retryCount;
  }
}