package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.transport.EtcdClientTransport;

import java.util.concurrent.TimeUnit;

/**
 * An Etcd Key Get Request
 */
public class EtcdKeyGetRequest extends EtcdKeyRequest {
  private boolean wait = false;

  /**
   * Constructs an EtcdKeysGetRequest
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyGetRequest(EtcdClientTransport clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler);
  }

  /**
   * Constructs an EtcdKeysGetRequest
   *
   * @param clientImpl   the client to handle this request
   * @param key          to get
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyGetRequest(EtcdClientTransport clientImpl, String key, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler, key);
  }

  /**
   * Set if server should wait
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest waitForChange() {
    this.wait = true;
    this.requestParams.put("wait", "true");
    return this;
  }

  /**
   * Set that server should wait for a certain change index
   *
   * @param waitIndex to wait for
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest waitForChange(long waitIndex) {
    this.waitForChange();
    this.requestParams.put("waitIndex", waitIndex + "");
    return this;
  }

  /**
   * Set that server should return values sorted
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest sorted() {
    this.requestParams.put("sorted", "true");
    return this;
  }

  /**
   * Set if operation is on a directory
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest dir() {
    this.requestParams.put("dir", "true");
    return this;
  }

  /**
   * Set if operation is recursive
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest recursive() {
    this.requestParams.put("recursive", "true");
    return this;
  }

  /**
   * Set if operation is consistent
   * (Etcd will use the master to be sure of latest value)
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest consistent() {
    this.requestParams.put("consistent", "true");
    return this;
  }

  /**
   * Get if command should be waiting
   *
   * @return true if it should wait
   */
  public boolean shouldBeWaiting() {
    return this.wait;
  }

  @Override public EtcdKeyGetRequest timeout(long timeout, TimeUnit unit) {
    super.timeout(timeout, unit);
    return this;
  }

  @Override public EtcdKeyGetRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }
}