package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdStoreStatsResponse;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;

/**
 * An Etcd Store Stats Request
 */
public class EtcdStoreStatsRequest extends EtcdRequest<EtcdStoreStatsResponse> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdStoreStatsRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler);
  }

  @Override public EtcdResponsePromise<EtcdStoreStatsResponse> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public EtcdStoreStatsRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public String getUri() {
    return "/v2/stats/store";
  }
}