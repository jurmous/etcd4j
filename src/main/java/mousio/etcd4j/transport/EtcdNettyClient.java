/*
 * Copyright (c) 2015, Jurriaan Mous and contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mousio.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsServerAddresses;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import mousio.client.ConnectionState;
import mousio.client.retry.RetryHandler;
import mousio.etcd4j.EtcdSecurityContext;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 *
 * Netty client for the requests and responses
 */
public class EtcdNettyClient implements EtcdClientImpl {
  private static final Logger logger = LoggerFactory.getLogger(EtcdNettyClient.class);

  // default etcd port
  private static final int DEFAULT_PORT = 2379;
  private static final String ENV_ETCD4J_ENDPOINT = "ETCD4J_ENDPOINT";
  private final EventLoopGroup eventLoopGroup;
  private final URI[] uris;

  private final Bootstrap bootstrap;
  //private final String hostName;
  private final EtcdNettyConfig config;
  private final EtcdSecurityContext securityContext;
  private final HashedWheelTimer timer;

  protected volatile int lastWorkingUriIndex;

  /**
   * Constructor
   *
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uri        to connect to
   */
  public EtcdNettyClient(final SslContext sslContext, final URI... uri) {
    this(new EtcdNettyConfig(), sslContext, uri);
  }

  /**
   * Constructor
   *
   * @param securityContext security context.
   * @param uri             to connect to
   */
  public EtcdNettyClient(final EtcdSecurityContext securityContext, final URI... uri) {
    this(new EtcdNettyConfig(), securityContext, uri);
  }

  /**
   * Constructor with custom eventloop group and timeout
   *
   * @param config     for netty
   * @param sslContext SSL context if connecting with SSL. Null if not connecting with SSL.
   * @param uris       to connect to
   */
  public EtcdNettyClient(final EtcdNettyConfig config,
                         final SslContext sslContext, final URI... uris) {
    this(config, new EtcdSecurityContext(sslContext), uris);
  }

  /**
   * Constructor with custom eventloop group and timeout
   *
   * @param config     for netty
   * @param uris       to connect to
   */
  public EtcdNettyClient(final EtcdNettyConfig config, final URI... uris) {
    this(config, EtcdSecurityContext.NONE, uris);
  }

  /**
   * Constructor with custom eventloop group and timeout
   *
   * @param config          for netty
   * @param securityContext security context (ssl, authentication)
   * @param uris            to connect to
   */
  public EtcdNettyClient(final EtcdNettyConfig config,
                         final EtcdSecurityContext securityContext, final URI... uris) {
    logger.info("Setting up Etcd4j Netty client");

    this.lastWorkingUriIndex = 0;
    this.config = config.clone();
    this.securityContext = securityContext.clone();
    this.uris = uris;
    this.timer = config.getTimer();
    this.eventLoopGroup = config.getEventLoopGroup();
    this.bootstrap = new Bootstrap()
      .group(eventLoopGroup)
      .channel(config.getSocketChannelClass())
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
      .resolver(new DnsAddressResolverGroup(
        NioDatagramChannel.class,
        DnsServerAddresses.defaultAddresses()))
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          if (securityContext.hasNettySsl()) {
            p.addLast(securityContext.nettySslContext().newHandler(ch.alloc()));
          } else if (securityContext.hasSsl()) {
            p.addLast(new SslHandler(securityContext.sslContext().createSSLEngine()));
          }
          p.addLast("codec", new HttpClientCodec());
          p.addLast("auth", new HttpBasicAuthHandler());
          p.addLast("chunkedWriter", new ChunkedWriteHandler());
          p.addLast("aggregate", new HttpObjectAggregator(config.getMaxFrameSize()));
        }
      });
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
    ConnectionState connectionState = new ConnectionState(timer, uris, lastWorkingUriIndex);

    if (etcdRequest.getPromise() == null) {
      etcdRequest.setPromise(new EtcdResponsePromise<R>(
        etcdRequest.getRetryPolicy(),
        connectionState,
        new RetryHandler() {
          @Override
          public void doRetry(ConnectionState connectionState) throws IOException {
            connect(etcdRequest, connectionState);
          }
      }));
    }

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
    if(eventLoopGroup.isShuttingDown() || eventLoopGroup.isShutdown() || eventLoopGroup.isTerminated()){
      etcdRequest.getPromise().getNettyPromise().cancel(true);
      logger.debug("Retry canceled because of closed etcd client");
      return;
    }

    final URI uri;

    // when we are called from a redirect, the url in the request may also
    // contain host and port!
    URI requestUri = URI.create(etcdRequest.getUrl());
    if (requestUri.getHost() != null && requestUri.getPort() > -1) {
      uri = requestUri;
    } else if (connectionState.uris.length == 0 && System.getenv(ENV_ETCD4J_ENDPOINT) != null) {
      // read uri from environment variable
      String endpoint_uri = System.getenv(ENV_ETCD4J_ENDPOINT);
      if(logger.isDebugEnabled()) {
        logger.debug("Will use environment variable {} as uri with value {}", ENV_ETCD4J_ENDPOINT, endpoint_uri);
      }
      uri = URI.create(endpoint_uri);
    } else {
      uri = connectionState.uris[connectionState.uriIndex];
    }

    // Start the connection attempt.
    final ChannelFuture connectFuture = bootstrap.connect(connectAddress(uri));
    etcdRequest.getPromise().attachNettyPromise(connectFuture.channel().eventLoop().<R>newPromise());

    connectFuture.addListener(new GenericFutureListener<ChannelFuture>() {
      @Override
      public void operationComplete(final ChannelFuture f) throws Exception {
        if (!f.isSuccess()) {
          final Throwable cause = f.cause();
          if (logger.isDebugEnabled()) {
            logger.debug("Connection failed to {}, cause {}", connectionState.uris[connectionState.uriIndex], cause);
          }

          if (cause instanceof ClosedChannelException || cause instanceof IllegalStateException) {
            etcdRequest.getPromise().cancel(new CancellationException("Channel closed"));
          } else {
            etcdRequest.getPromise().handleRetry(f.cause());
          }

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
          logger.debug("Connected to {} ({})", f.channel().remoteAddress().toString(), connectionState.uriIndex);
        }

        lastWorkingUriIndex = connectionState.uriIndex;

        modifyPipeLine(etcdRequest, f.channel().pipeline());

        createAndSendHttpRequest(uri, etcdRequest.getUrl(), etcdRequest, f.channel())
          .addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
              if (!future.isSuccess()) {
                etcdRequest.getPromise().setException(future.cause());
                if (!f.channel().eventLoop().inEventLoop()) {
                  f.channel().eventLoop().shutdownGracefully();
                }

                f.channel().close();
              }
            }
          });

        f.channel().closeFuture().addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (logger.isDebugEnabled()) {
              logger.debug("Connection closed for request {} on uri {} ",
              etcdRequest.getMethod().name(),
              etcdRequest.getUri());
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
  private <R> void modifyPipeLine(final EtcdRequest<R> req, final ChannelPipeline pipeline) {
    final EtcdResponseHandler<R> handler = new EtcdResponseHandler<>(this, req);

    if (req.hasTimeout()) {
      pipeline.addFirst(new ReadTimeoutHandler(req.getTimeout(), req.getTimeoutUnit()));
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
    httpRequest.headers().add(HttpHeaderNames.CONNECTION, "keep-alive");
    if(!this.config.hasHostName()) {
      httpRequest.headers().add(HttpHeaderNames.HOST, server.getHost() + ":" + server.getPort());
    } else {
      httpRequest.headers().add(HttpHeaderNames.HOST, this.config.getHostName());
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
        QueryStringEncoder encoder = new QueryStringEncoder(uri);
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
          encoder.addParam(entry.getKey() , entry.getValue());
        }

        httpRequest.setUri(encoder.toString());
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
  @Override
  public void close() {
    logger.info("Shutting down Etcd4j Netty client");

    if (config.isManagedEventLoopGroup()) {
      logger.debug("Shutting down Netty Loop");
      eventLoopGroup.shutdownGracefully();
    }

    if (config.isManagedTimer()) {
      logger.debug("Shutting down Netty Timer");
      timer.stop();
    }
  }

  private InetSocketAddress connectAddress(URI uri) {
    return InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort());
  }

  private class HttpBasicAuthHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (securityContext.hasCredentials() && msg instanceof HttpRequest) {
        addBasicAuthHeader((HttpRequest)msg);
      }

      ctx.write(msg, promise);
    }

    private void addBasicAuthHeader(HttpRequest request) {
      final String auth = Base64.encode(
        Unpooled.copiedBuffer(
          securityContext.username() + ":" + securityContext.password(),
          CharsetUtil.UTF_8)
        ).toString(CharsetUtil.UTF_8);

      request.headers().add(HttpHeaderNames.AUTHORIZATION, "Basic " + auth);
    }
  }
}
