package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdSelfStatsResponse;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;

/**
 * An Etcd Store Stats Request
 */
public class EtcdSelfStatsRequest extends EtcdRequest<EtcdSelfStatsResponse> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdSelfStatsRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler);
  }

  @Override public EtcdResponsePromise<EtcdSelfStatsResponse> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public EtcdSelfStatsRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public String getUri() {
    return "/v2/stats/self";
  }
}