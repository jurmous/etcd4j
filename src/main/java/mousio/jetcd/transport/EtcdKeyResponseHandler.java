package mousio.jetcd.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.jetcd.responses.EtcdKeysResponse;
import mousio.jetcd.responses.EtcdKeysResponseParser;

/**
 * Handles etcd responses
 */
public class EtcdKeyResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
  private final Promise<EtcdKeysResponse> promise;

  /**
   * Constructor
   *
   * @param responsePromise promise to work on.
   */
  public EtcdKeyResponseHandler(Promise<EtcdKeysResponse> responsePromise) {
    this.promise = responsePromise;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    if (!response.content().isReadable()) {
      if (response.status().equals(HttpResponseStatus.OK)) {
        return;
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
