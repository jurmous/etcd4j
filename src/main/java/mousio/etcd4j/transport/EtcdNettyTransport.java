package mousio.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import mousio.client.ConnectionState;
import mousio.client.retry.RetryHandler;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.requests.EtcdOldVersionRequest;
import mousio.etcd4j.requests.EtcdRequest;
import mousio.etcd4j.requests.EtcdVersionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * Netty client for the requests and responses
 */
public class EtcdNettyTransport implements EtcdClientTransport {
  private static final Logger logger = LoggerFactory.getLogger(EtcdNettyTransport.class);

  private final EventLoopGroup eventLoopGroup;
  private final URI[] uris;

  private final Bootstrap bootstrap;
  private final String hostName;

  protected int lastWorkingUriIndex = 0;

  /**
   * Constructor
   *
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uri        to connect to
   */
  public EtcdNettyTransport(final SslContext sslContext, final URI... uri) {
    this(new EtcdNettyTransportConfig(), sslContext, uri);
  }

  /**
   * Constructor with custom eventloop group and timeout
   *
   * @param config     for netty
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uris       to connect to
   */
  public EtcdNettyTransport(final EtcdNettyTransportConfig config,
                            final SslContext sslContext, final URI... uris) {
    logger.info("Setting up Etcd4j Netty client");

    this.uris = uris;
    this.eventLoopGroup = config.getEventLoopGroup();
    this.bootstrap = new Bootstrap()
        .group(eventLoopGroup)
        .channel(config.getSocketChannelClass())
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
              p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast("codec", new HttpClientCodec());
            p.addLast("chunkedWriter", new ChunkedWriteHandler());
            p.addLast("aggregate", new HttpObjectAggregator(config.getMaxFrameSize()));
          }
        });

    this.hostName = config.getHostName();
  }

  /**
   * For tests
   *
   * @return the current bootstrap
   */
  protected Bootstrap getBootstrap() {
    return bootstrap;
  }

  /**
   * Send a request and get a future.
   *
   * @param etcdRequest Etcd Request to send
   * @return Promise for the request.
   */
  public <R> EtcdResponsePromise<R> send(final EtcdRequest<R> etcdRequest) throws IOException {
    final ConnectionState connectionState = new ConnectionState(uris);
    connectionState.uriIndex = lastWorkingUriIndex;

    if (etcdRequest.getPromise() == null) {
      EtcdResponsePromise<R> responsePromise = new EtcdResponsePromise<>(etcdRequest.getRetryPolicy(), connectionState, new RetryHandler() {
        @Override
        public void doRetry() throws IOException {
          connect(etcdRequest, connectionState);
        }
      });
      etcdRequest.setPromise(responsePromise);
    }

    connectionState.startTime = new Date().getTime();
    connect(etcdRequest, connectionState);

    return etcdRequest.getPromise();
  }

  /**
   * Connect to server
   *
   * @param etcdRequest to request with
   * @param <R>         Type of response
   * @throws IOException if request could not be sent.
   */
  @SuppressWarnings("unchecked")
  protected <R> void connect(final EtcdRequest<R> etcdRequest) throws IOException {
    this.connect(etcdRequest, etcdRequest.getPromise().getConnectionState());
  }

  /**
   * Connect to server
   *
   * @param etcdRequest     to request with
   * @param connectionState for retries
   * @param <R>             Type of response
   * @throws IOException if request could not be sent.
   */
  @SuppressWarnings("unchecked")
  protected <R> void connect(final EtcdRequest<R> etcdRequest, final ConnectionState connectionState) throws IOException {
    final URI uri;

    // when we are called from a redirect, the url in the request may also
    // contain host and port!
    URI requestUri = URI.create(etcdRequest.getUrl());
    if (requestUri.getHost() != null && requestUri.getPort() > -1) {
      uri = requestUri;
    }else{
      uri = uris[connectionState.uriIndex];
    }

    // Start the connection attempt.
    final ChannelFuture connectFuture = bootstrap.clone().connect(uri.getHost(), uri.getPort());

    final Channel channel = connectFuture.channel();

    etcdRequest.getPromise().attachNettyPromise((Promise<R>) new DefaultPromise<>(connectFuture.channel().eventLoop()));

    connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
      @Override
      public void operationComplete(final ChannelFuture f) throws Exception {
        if (!f.isSuccess()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Connection failed to " + connectionState.uris[connectionState.uriIndex]);
          }
          etcdRequest.getPromise().handleRetry(f.cause());
          return;
        }

        // Handle already cancelled promises
        if (etcdRequest.getPromise().getNettyPromise().isCancelled()) {
          f.channel().close();
          etcdRequest.getPromise().getNettyPromise().setFailure(new CancellationException());
          return;
        }

        final Promise listenedToPromise = etcdRequest.getPromise().getNettyPromise();

        // Close channel when promise is satisfied or cancelled later
        listenedToPromise.addListener(new GenericFutureListener<Future<?>>() {
          @Override
          public void operationComplete(Future<?> future) throws Exception {
            // Only close if it was not redirected to new promise
            if (etcdRequest.getPromise().getNettyPromise() == listenedToPromise) {
              f.channel().close();
            }
          }
        });

        if (logger.isDebugEnabled()) {
          logger.debug("Connected to " + channel.remoteAddress().toString());
        }

        lastWorkingUriIndex = connectionState.uriIndex;

        modifyPipeLine(etcdRequest, f.channel().pipeline());

        createAndSendHttpRequest(uri, etcdRequest.getUrl(), etcdRequest, channel)
            .addListener(new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  etcdRequest.getPromise().setException(future.cause());
                  f.channel().close();
                }
              }
            });

        channel.closeFuture().addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (logger.isDebugEnabled()) {
              logger.debug("Connection closed for request " + etcdRequest.getMethod().name() + " " + etcdRequest.getUri());
            }
          }
        });
      }
    });
  }

  /**
   * Modify the pipeline for the request
   *
   * @param req      to process
   * @param pipeline to modify
   * @param <R>      Type of Response
   */
  @SuppressWarnings("unchecked")
  private <R> void modifyPipeLine(final EtcdRequest<R> req, final ChannelPipeline pipeline) {
    if (req.getTimeout() != -1) {
      pipeline.addFirst(new ReadTimeoutHandler(req.getTimeout(), req.getTimeoutUnit()));
    }

    final AbstractEtcdResponseHandler handler;

    if (req instanceof EtcdKeyRequest) {
      handler = new EtcdKeyResponseHandler(this, (EtcdKeyRequest) req);
    } else if (req instanceof EtcdVersionRequest) {
      handler = new EtcdVersionResponseHandler(this, (EtcdVersionRequest) req);
    } else if (req instanceof EtcdOldVersionRequest) {
      handler = new AbstractEtcdResponseHandler<EtcdOldVersionRequest, FullHttpResponse>(this, (EtcdOldVersionRequest) req) {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
          (((EtcdOldVersionRequest) req).getPromise()).getNettyPromise().setSuccess(msg.content().toString(Charset.defaultCharset()));
        }

        @Override
        protected FullHttpResponse decodeResponse(FullHttpResponse response) throws Exception {
          return null;
        }
      };
    } else {
      throw new RuntimeException("Unknown request type " + req.getClass().getName());
    }

    pipeline.addLast(handler);
    pipeline.addLast(new ChannelHandlerAdapter() {
      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handler.retried(true);
        req.getPromise().handleRetry(cause);
      }
    });
  }

  /**
   * Get HttpRequest belonging to etcdRequest
   *
   * @param server      server for http request
   * @param uri         to send request to
   * @param etcdRequest to send
   * @param channel     to send request on
   * @param <R>         Response type
   * @return HttpRequest
   * @throws Exception when creating or sending HTTP request fails
   */
  private <R> ChannelFuture createAndSendHttpRequest(URI server, String uri, EtcdRequest<R> etcdRequest, Channel channel) throws Exception {
    HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, etcdRequest.getMethod(), uri);
    httpRequest.headers().add("Connection", "keep-alive");
    if(this.hostName == null) {
      httpRequest.headers().add("Host", server.getHost() + ":" + server.getPort());
    } else {
      httpRequest.headers().add("Host", this.hostName);
    }

    HttpPostRequestEncoder bodyRequestEncoder = null;
    Map<String, String> keyValuePairs = etcdRequest.getRequestParams();
    if (keyValuePairs != null && !keyValuePairs.isEmpty()) {
      HttpMethod etcdRequestMethod = etcdRequest.getMethod();
      if (etcdRequestMethod == HttpMethod.POST || etcdRequestMethod == HttpMethod.PUT) {
        bodyRequestEncoder = new HttpPostRequestEncoder(httpRequest, false);
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
          bodyRequestEncoder.addBodyAttribute(entry.getKey(), entry.getValue());
        }

        httpRequest = bodyRequestEncoder.finalizeRequest();
      } else {
        String getLocation = "";
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
          if (!getLocation.isEmpty()) {
            getLocation += "&";
          }
          getLocation += entry.getKey() + "=" + entry.getValue();
        }

        if (!uri.contains("?")) {
          httpRequest.setUri(uri.concat("?").concat(getLocation));
        } else {
          httpRequest.setUri(uri);
        }
      }
    }
    etcdRequest.setHttpRequest(httpRequest);
    ChannelFuture future = channel.write(httpRequest);
    if (bodyRequestEncoder != null && bodyRequestEncoder.isChunked()) {
      future = channel.write(bodyRequestEncoder);
    }
    channel.flush();
    return future;
  }

  /**
   * Close netty
   */
  public void close() {
    logger.info("Shutting down Etcd4j Netty client");
    eventLoopGroup.shutdownGracefully();
  }
}