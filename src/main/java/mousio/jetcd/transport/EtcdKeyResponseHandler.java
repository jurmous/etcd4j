package mousio.jetcd.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.jetcd.requests.EtcdKeyRequest;
import mousio.jetcd.responses.EtcdKeysResponse;
import mousio.jetcd.responses.EtcdKeysResponseParser;

/**
 * Handles etcd responses
 */
public class EtcdKeyResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
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
    if (!response.content().isReadable()) {
      if (response.status().equals(HttpResponseStatus.OK)) {
        this.client.connect(this.request);
        return;
      } else if (response.status().equals(HttpResponseStatus.MOVED_PERMANENTLY)
          || response.status().equals(HttpResponseStatus.TEMPORARY_REDIRECT)) {
        if (response.headers().contains("Location")) {
          this.client.connect(this.request, response.headers().get("Location"));
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
      this.promise.setSuccess(
          EtcdKeysResponseParser.parse(response.content())
      );
    }
    // Catches both parsed EtcdExceptions and parsing exceptions
    catch (Exception e) {
      this.promise.setFailure(e);
    }
  }
}
