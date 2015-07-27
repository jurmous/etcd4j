package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.transport.EtcdClientTransport;

import java.util.concurrent.TimeUnit;

/**
 * An Etcd Key Post Request
 */
public class EtcdKeyPostRequest extends EtcdKeyRequest {

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param key          key to change
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyPostRequest(EtcdClientTransport clientImpl, String key, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.POST, retryHandler, key);
  }

  /**
   * Set the value for the request
   *
   * @param value to set on key
   * @return Itself for chaining
   */
  public EtcdKeyPostRequest value(String value) {
    this.requestParams.put("value", value);
    return this;
  }

  /**
   * Set the Time to live for a key in seconds
   *
   * @param ttl time to live in seconds
   * @return Itself for chaining
   */
  public EtcdKeyPostRequest ttl(Integer ttl) {
    this.requestParams.put("ttl", (ttl == null) ? "" : ttl + "");
    return this;
  }

  @Override public EtcdKeyPostRequest timeout(long timeout, TimeUnit unit) {
    super.timeout(timeout, unit);
    return this;
  }

  @Override public EtcdKeyPostRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }
}