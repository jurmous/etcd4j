package mousio.jetcd.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.transport.EtcdClientImpl;

import java.io.IOException;

/**
 * An Etcd Version Request
 */
public class EtcdVersionRequest extends EtcdRequest<String> {

  /**
   * Constructor
   *
   * @param clientImpl the client to handle this request
   */
  public EtcdVersionRequest(EtcdClientImpl clientImpl) {
    super(clientImpl, HttpMethod.GET);
  }

  @Override public EtcdResponsePromise<String> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public String getUri() {
    return "/version";
  }
}