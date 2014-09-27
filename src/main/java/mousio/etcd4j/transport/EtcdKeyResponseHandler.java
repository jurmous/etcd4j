package mousio.etcd4j.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponseParser;
import mousio.etcd4j.responses.PrematureDisconnection;

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
          System.out.println("WARNING: redirect for " + this.request.getHttpRequest().uri() + " to " + response.headers().get("Location"));
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
      this.promise.setSuccess(
          EtcdKeysResponseParser.parse(response.content())
      );
    }
    // Catches both parsed EtcdExceptions and parsing exceptions
    catch (Exception e) {
      this.promise.setFailure(e);
    }
  }

  @Override public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!promise.isDone()) {
      promise.setFailure(new PrematureDisconnection());
    }
  }
}
