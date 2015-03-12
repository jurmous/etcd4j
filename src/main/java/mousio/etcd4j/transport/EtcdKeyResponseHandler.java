package mousio.etcd4j.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles etcd responses
 */
public class EtcdKeyResponseHandler extends AbstractEtcdResponseHandler<EtcdKeyRequest, FullHttpResponse> {
  private static final Logger logger = LoggerFactory.getLogger(EtcdKeyResponseHandler.class);

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdKeyRequest  request
   */
  @SuppressWarnings("unchecked")
  public EtcdKeyResponseHandler(EtcdNettyClient etcdNettyClient, EtcdKeyRequest etcdKeyRequest) {
    super(etcdNettyClient, etcdKeyRequest);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Received " + response.status().code() + " for " + this.request.getMethod().name() + " "
              + this.request.getUri());
    }

    if (response.status().equals(HttpResponseStatus.OK)
        || response.status().equals(HttpResponseStatus.ACCEPTED)
        || response.status().equals(HttpResponseStatus.CREATED)) {
      if (!response.content().isReadable()) {
        this.client.connect(this.request);
        return;
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
    } else if (response.status().equals(HttpResponseStatus.MOVED_PERMANENTLY)
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
      try {
        EtcdKeysResponse etcdKeysResponse = EtcdKeysResponseParser.parse(response.content());
        this.promise.setSuccess(etcdKeysResponse);
      }
      // Catches both parsed EtcdExceptions and parsing exceptions
      catch (Exception e) {
        this.promise.setFailure(e);
      }
    }
  }
}
