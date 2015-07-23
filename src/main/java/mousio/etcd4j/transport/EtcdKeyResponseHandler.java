package mousio.etcd4j.transport;

import io.netty.handler.codec.http.FullHttpResponse;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponseParser;

/**
 * Handles etcd responses
 */
public class EtcdKeyResponseHandler extends AbstractEtcdResponseHandler<EtcdKeyRequest, EtcdKeysResponse> {
  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdKeyRequest  request
   */
  public EtcdKeyResponseHandler(EtcdNettyClient etcdNettyClient, EtcdKeyRequest etcdKeyRequest) {
    super(etcdNettyClient, etcdKeyRequest);
  }

  @Override
  protected EtcdKeysResponse decodeResponse(FullHttpResponse response) throws Exception {
    return EtcdKeysResponseParser.parse(response.headers(),response.content());
  }
}
