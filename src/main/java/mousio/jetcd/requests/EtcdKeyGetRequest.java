package mousio.jetcd.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.transport.EtcdClientImpl;

/**
 * An Etcd Key Get Request
 */
public class EtcdKeyGetRequest extends EtcdKeyRequest {
  private boolean shouldWait = false;

  /**
   * Constructs an EtcdKeysGetRequest
   *
   * @param clientImpl the client to handle this request
   */
  public EtcdKeyGetRequest(EtcdClientImpl clientImpl) {
    super(clientImpl, HttpMethod.GET);
  }

  /**
   * Constructs an EtcdKeysGetRequest
   *
   * @param clientImpl the client to handle this request
   * @param key        to get
   */
  public EtcdKeyGetRequest(EtcdClientImpl clientImpl, String key) {
    super(clientImpl, HttpMethod.GET);
    this.setKey(key);
  }

  /**
   * Set if server should wait
   *
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest waitForChange() {
    this.shouldWait = true;
    this.requestParams.put("wait", "true");
    return this;
  }

  /**
   * Set that server should wait for a certain change index
   *
   * @param waitIndex to wait for
   * @return Itself for chaining
   */
  public EtcdKeyGetRequest waitForChange(int waitIndex) {
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

  public boolean shouldBeWaiting() {
    return this.shouldWait;
  }
}