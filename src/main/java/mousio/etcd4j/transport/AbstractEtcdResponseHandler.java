package mousio.etcd4j.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.client.exceptions.PrematureDisconnectException;
import mousio.etcd4j.requests.EtcdRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles etcd responses
 *
 * @param <RQ> Request type
 * @param <RS> Response type
 */
abstract class AbstractEtcdResponseHandler<RQ extends EtcdRequest, RS> extends SimpleChannelInboundHandler<FullHttpResponse> {
  private static final Logger logger = LoggerFactory.getLogger(AbstractEtcdResponseHandler.class);

  protected final Promise<RS> promise;
  protected final EtcdNettyTransport client;
  protected final RQ request;

  private boolean isRetried;

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdRequest     request
   */
  @SuppressWarnings("unchecked")
  public AbstractEtcdResponseHandler(EtcdNettyTransport etcdNettyClient, RQ etcdRequest) {
    this.client = etcdNettyClient;
    this.request = etcdRequest;
    this.promise = etcdRequest.getPromise().getNettyPromise();
    this.isRetried = false;
  }

  /**
   * Set if the connection is retried.
   * If true the promise will not fail on unregistering this handler.
   *
   * @param retried true if request is being retried.
   */
  public void retried(boolean retried) {
    this.isRetried = retried;
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!isRetried && !promise.isDone()) {
      this.request.getPromise().handleRetry(new PrematureDisconnectException());
    }
    super.channelUnregistered(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug(
        "Received " + response.status().code() + " for " + this.request.getMethod().name() + " "
          + this.request.getUri());
    }

    if (response.status().equals(HttpResponseStatus.MOVED_PERMANENTLY)
      || response.status().equals(HttpResponseStatus.TEMPORARY_REDIRECT)) {
      if (response.headers().contains("Location")) {
        this.request.setUrl(response.headers().get("Location"));
        this.client.connect(this.request);
        // Closing the connection which handled the previous request.
        ctx.close();
        if (logger.isDebugEnabled()) {
          logger.debug(
            "redirect for " + this.request.getHttpRequest().uri() + " to " + response.headers()
              .get("Location"));
        }
      } else {
        this.promise.setFailure(new Exception("Missing Location header on redirect"));
      }
    } else {
      if (!response.content().isReadable()) {
        this.promise.setFailure(new IOException("Content was not readable. HTTP Status: "
          + response.status()));
      }

      try {
        this.promise.setSuccess(decodeResponse(response));
      }
      // Catches both parsed EtcdExceptions and parsing exceptions
      catch (Exception e) {
        this.promise.setFailure(e);
      }
    }
  }

  protected abstract RS decodeResponse(FullHttpResponse response) throws Exception;
}
