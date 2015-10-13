package mousio.etcd4j.transport;

import io.netty.handler.codec.http.FullHttpResponse;
import mousio.etcd4j.requests.EtcdOldVersionRequest;

import java.nio.charset.Charset;

public class EtcdOldVersionResponseHandler extends AbstractEtcdResponseHandler<EtcdOldVersionRequest, String> {
  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdVersionRequest request
   */
  public EtcdOldVersionResponseHandler(EtcdNettyClient etcdNettyClient, EtcdOldVersionRequest etcdVersionRequest) {
    super(etcdNettyClient, etcdVersionRequest);
  }

  @Override
  protected String decodeResponse(FullHttpResponse response) throws Exception {
    return response.content().toString(Charset.defaultCharset());
  }
}
