package mousio.jetcd.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.transport.EtcdClientImpl;

import java.util.concurrent.TimeUnit;

/**
 * An Etcd Key Delete Request
 */
public class EtcdKeyDeleteRequest extends EtcdKeyRequest {

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl the client to handle this request
   * @param key        key to change
   */
  public EtcdKeyDeleteRequest(EtcdClientImpl clientImpl, String key) {
    super(clientImpl, HttpMethod.DELETE);
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
  public EtcdKeyDeleteRequest prevIndex(int prevIndex) {
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

  @Override public EtcdKeyDeleteRequest timeout(int timeout, TimeUnit unit) {
    super.timeout(timeout, unit);
    return this;
  }
}