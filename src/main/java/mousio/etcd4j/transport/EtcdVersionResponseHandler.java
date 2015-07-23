package mousio.etcd4j.transport;

import io.netty.handler.codec.http.FullHttpResponse;
import mousio.etcd4j.requests.EtcdVersionRequest;
import mousio.etcd4j.responses.EtcdVersionResponse;
import mousio.etcd4j.responses.EtcdVersionResponseParser;

public class EtcdVersionResponseHandler extends AbstractEtcdResponseHandler<EtcdVersionRequest, EtcdVersionResponse> {

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdVersionRequest request
   */
  public EtcdVersionResponseHandler(EtcdNettyClient etcdNettyClient, EtcdVersionRequest etcdVersionRequest) {
    super(etcdNettyClient, etcdVersionRequest);
  }

  @Override
  protected EtcdVersionResponse decodeResponse(FullHttpResponse response) throws Exception {
    return EtcdVersionResponseParser.parse(response.headers(),response.content());
  }
}
