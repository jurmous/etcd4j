package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.util.concurrent.TimeUnit;

/**
 * An Etcd Key Delete Request
 */
public class EtcdKeyDeleteRequest extends EtcdKeyRequest {

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param key          key to change
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyDeleteRequest(EtcdClientImpl clientImpl, String key, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.DELETE, retryHandler);
    this.setKey(key);
  }

  /**
   * Set the prev value to check
   *
   * @param value to set on key
   * @return Itself for chaining
   */
  public EtcdKeyDeleteRequest prevValue(String value) {
    this.requestParams.put("prevValue", value);
    return this;
  }

  /**
   * Set the previous index to check
   *
   * @param prevIndex to set on key
   * @return Itself for chaining
   */
  public EtcdKeyDeleteRequest prevIndex(long prevIndex) {
    this.requestParams.put("prevIndex", prevIndex + "");
    return this;
  }

  /**
   * Set that operation is on a directory
   *
   * @return Itself for chaining
   */
  public EtcdKeyDeleteRequest dir() {
    this.requestParams.put("dir", "true");
    return this;
  }

  /**
   * Set that operation is recursive
   *
   * @return Itself for chaining
   */
  public EtcdKeyDeleteRequest recursive() {
    this.requestParams.put("recursive", "true");
    return this;
  }

  @Override public EtcdKeyDeleteRequest timeout(long timeout, TimeUnit unit) {
    super.timeout(timeout, unit);
    return this;
  }

  @Override public EtcdKeyDeleteRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }
}