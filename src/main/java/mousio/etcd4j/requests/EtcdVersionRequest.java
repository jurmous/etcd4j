package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.transport.EtcdClientImpl;

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