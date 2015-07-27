package mousio.etcd4j.requests;


import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic Etcd Keys Request
 */
public class EtcdKeyRequest extends EtcdRequest<EtcdKeysResponse> {
  protected final String key;
  protected final Map<String, String> requestParams;

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param method       to set request with
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyRequest(EtcdClientImpl clientImpl, HttpMethod method, RetryPolicy retryHandler) {
    this(clientImpl, method, retryHandler, null);
  }

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param method       to set request with
   * @param retryHandler handles retries on fails
   * @param key          key to do action on
   */
  public EtcdKeyRequest(EtcdClientImpl clientImpl, HttpMethod method, RetryPolicy retryHandler, String key) {
    super(clientImpl, method, retryHandler);

    if (key.startsWith("/")){
      key = key.substring(1);
    }

    this.key = key;
    this.requestParams = new HashMap<>();
  }

  @Override public EtcdKeyRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public EtcdResponsePromise<EtcdKeysResponse> send() throws IOException {
    return this.clientImpl.send(this);
  }

  @Override public String getUri() {
    return "/v2/keys/" + ((key != null) ? key : "");
  }

  @Override public Map<String, String> getRequestParams() {
    return requestParams;
  }
}