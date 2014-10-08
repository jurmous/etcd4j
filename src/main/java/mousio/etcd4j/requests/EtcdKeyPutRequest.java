package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.util.concurrent.TimeUnit;

/**
 * An Etcd Key Put Request
 */
public class EtcdKeyPutRequest extends EtcdKeyRequest {

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param key          key to change
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyPutRequest(EtcdClientImpl clientImpl, String key, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.PUT, retryHandler);
    this.setKey(key);
  }

  /**
   * Set the value for the request
   *
   * @param value to set on key
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest value(String value) {
    this.requestParams.put("value", value);
    return this;
  }

  /**
   * Set the Time to live for a key in seconds
   *
   * @param ttl time to live in seconds
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest ttl(Integer ttl) {
    this.requestParams.put("ttl", (ttl == null) ? "" : ttl + "");
    return this;
  }

  /**
   * Set that operation is on a directory
   *
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest isDir() {
    this.requestParams.put("dir", "true");
    return this;
  }

  /**
   * Set that previous value should exist
   *
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest prevExist() {
    this.requestParams.put("prevExist", "true");
    return this;
  }

  /**
   * Set the previous index to check
   *
   * @param prevIndex to set on key
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest prevIndex(long prevIndex) {
    this.requestParams.put("prevIndex", prevIndex + "");
    return this;
  }


  /**
   * Set the prev value to check
   *
   * @param value to set on key
   * @return Itself for chaining
   */
  public EtcdKeyPutRequest prevValue(String value) {
    this.requestParams.put("prevValue", value);
    return this;
  }

  @Override public EtcdKeyPutRequest timeout(long timeout, TimeUnit unit) {
    super.timeout(timeout, unit);
    return this;
  }

  @Override public EtcdKeyPutRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }
}