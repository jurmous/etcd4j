package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdVersionResponse;
import mousio.etcd4j.transport.EtcdClientTransport;

import java.io.IOException;

/**
 * An Etcd Version Request
 */
public class EtcdVersionRequest extends EtcdRequest<EtcdVersionResponse> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdVersionRequest(EtcdClientTransport clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler);
  }

  @Override public EtcdResponsePromise<EtcdVersionResponse> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public EtcdVersionRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public String getUri() {
    return "/version";
  }
}