package mousio.etcd4j.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.client.exceptions.PrematureDisconnectException;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles etcd responses
 */
public class EtcdKeyResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
  private static final Logger logger = LoggerFactory.getLogger(EtcdKeyResponseHandler.class);

  private final Promise<EtcdKeysResponse> promise;
  private final EtcdNettyClient client;
  private final EtcdKeyRequest request;

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdKeyRequest  request
   */
  @SuppressWarnings("unchecked")
  public EtcdKeyResponseHandler(EtcdNettyClient etcdNettyClient, EtcdKeyRequest etcdKeyRequest) {
    this.client = etcdNettyClient;
    this.request = etcdKeyRequest;
    this.promise = etcdKeyRequest.getPromise().getNettyPromise();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    logger.info("Received " + response.status().code() + " for " + this.request.getMethod().name() + " " + this.request.getUri());

    if (!response.content().isReadable()) {
      if (response.status().equals(HttpResponseStatus.OK)) {
        this.client.connect(this.request);
        return;
      } else if (response.status().equals(HttpResponseStatus.MOVED_PERMANENTLY)
          || response.status().equals(HttpResponseStatus.TEMPORARY_REDIRECT)) {
        if (response.headers().contains("Location")) {
          this.request.setUrl(response.headers().get("Location"));
          this.client.connect(this.request);
          logger.warn("redirect for " + this.request.getHttpRequest().uri() + " to " + response.headers().get("Location"));
          return;
        } else {
          this.promise.setFailure(new Exception("Missing Location header on redirect"));
          return;
        }
      } else {
        this.promise.setFailure(new Exception(response.status().toString()));
        return;
      }
    }

    try {
      EtcdKeysResponse etcdKeysResponse = EtcdKeysResponseParser.parse(response.content());
      String etcdIndex = response.headers().get("X-Etcd-Index");
      if (etcdIndex != null) {
        try {
          etcdKeysResponse.etcdIndex = Long.parseLong(etcdIndex);
        } catch (Exception e) {
          logger.error("could not parse X-Etcd-Index header", e);
        }
      }

      this.promise.setSuccess(etcdKeysResponse);
    }
    // Catches both parsed EtcdExceptions and parsing exceptions
    catch (Exception e) {
      this.promise.setFailure(e);
    }
  }

  @Override public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!promise.isDone()) {
      this.request.getPromise().handleRetry(new PrematureDisconnectException());
    }
  }
}
