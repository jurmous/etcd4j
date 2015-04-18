package mousio.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CancellationException;

import mousio.client.ConnectionState;
import mousio.client.retry.RetryHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.requests.EtcdRequest;
import mousio.etcd4j.requests.EtcdVersionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty client for the requests and responses
 */
public class EtcdNettyClient implements EtcdClientImpl {
  private static final Logger logger = LoggerFactory.getLogger(EtcdNettyClient.class);
  private static final int DEFAULT_CONNECT_TIMEOUT = 300;

  private final Bootstrap bootstrap;
  private final NioEventLoopGroup eventLoopGroup;

  private final URI[] uris;
  protected int lastWorkingUriIndex = 0;

  /**
   * Constructor
   *
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uri to connect to
   */
  public EtcdNettyClient(final SslContext sslContext, final URI... uri) {
    logger.info("Setting up Etcd4j Netty client");

    this.eventLoopGroup = new NioEventLoopGroup();

    this.uris = uri;

    this.bootstrap = initClient(sslContext, eventLoopGroup, DEFAULT_CONNECT_TIMEOUT);
  }

  private EtcdNettyClient(NioEventLoopGroup eventLoopGroup, int connectTimeout, SslContext sslContext, final URI... uris) {
    this.uris = uris;
    this.eventLoopGroup = eventLoopGroup;
    this.bootstrap = initClient(sslContext, eventLoopGroup, connectTimeout);
  }

  private Bootstrap initClient(final SslContext sslContext, final NioEventLoopGroup eventLoopGroup, int connectTimeout) {
    // Configure the client.
    return new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout).handler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            if (sslContext != null) {
              p.addLast(sslContext.newHandler(ch.alloc()));
            }
            p.addLast("codec", new HttpClientCodec());
            p.addLast("chunkedWriter", new ChunkedWriteHandler());
            int maxFrameSize = Integer.parseInt(System.getProperty("mousio.etcd4j.maxFrameSize", Integer.toString(1024 * 100)));
            p.addLast("aggregate", new HttpObjectAggregator(maxFrameSize));
          }
        });
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
   * @param <R> Type of response
   * @throws IOException if request could not be sent.
   */
  @SuppressWarnings("unchecked")
  protected <R> void connect(final EtcdRequest<R> etcdRequest) throws IOException {
    this.connect(etcdRequest, etcdRequest.getPromise().getConnectionState());
  }

  /**
   * Connect to server
   *
   * @param etcdRequest to request with
   * @param connectionState for retries
   * @param <R> Type of response
   * @throws IOException if request could not be sent.
   */
  @SuppressWarnings("unchecked")
  protected <R> void connect(final EtcdRequest<R> etcdRequest, final ConnectionState connectionState) throws IOException {
    URI uri = uris[connectionState.uriIndex];

    // when we are called from a redirect, the url in the request may also
    // contain host and port!
    URI requestUri = URI.create(etcdRequest.getUrl());
    if (requestUri.getHost() != null && requestUri.getPort() > -1) {
      uri = requestUri;
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
            logger.debug(String.format("Connection failed to " + connectionState.uris[connectionState.uriIndex]));
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

        createAndSendHttpRequest(etcdRequest.getUrl(), etcdRequest, channel).addListener(new ChannelFutureListener() {
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
   * @param req to process
   * @param pipeline to modify
   * @param <R> Type of Response
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
      handler = new AbstractEtcdResponseHandler<EtcdVersionRequest, FullHttpResponse>(this, (EtcdVersionRequest) req) {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
          (((EtcdVersionRequest) req).getPromise()).getNettyPromise().setSuccess(msg.content().toString(Charset.defaultCharset()));
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
   * @param uri to send request to
   * @param etcdRequest to send
   * @param <R> Response type
   * @return HttpRequest
   * @throws Exception
   */
  private <R> ChannelFuture createAndSendHttpRequest(String uri, EtcdRequest<R> etcdRequest, Channel channel) throws Exception {
    HttpRequest httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, etcdRequest.getMethod(), uri);
    httpRequest.headers().add("Connection", "keep-alive");
    httpRequest.headers().add("Host", InetAddress.getLocalHost().getHostName());
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

  public static class Builder extends EtcdClient.AbstractEtcdClientBuilder<Builder> {

    private NioEventLoopGroup eventLoopGroupToUse;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    public Builder usingEventLoopGroup(NioEventLoopGroup eventLoopGroup) {
      if (eventLoopGroup == null) {
        throw new IllegalArgumentException("Provided event loop group cannot be null");
      }
      eventLoopGroupToUse = eventLoopGroup;
      return this;
    }

    public Builder withConnectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public EtcdClient build() {
      return new EtcdClient(new EtcdNettyClient(eventLoopGroupToUse, connectTimeout, sslContext, uris));
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}