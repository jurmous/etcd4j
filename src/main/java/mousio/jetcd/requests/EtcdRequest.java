package mousio.jetcd.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.transport.EtcdClientImpl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Request to Etcd
 *
 * @param <R> Response Type returned by request
 */
public abstract class EtcdRequest<R> {
  protected final EtcdClientImpl clientImpl;

  protected final HttpMethod method;
  private EtcdResponsePromise<R> promise;
  private int timeout = -1;
  private TimeUnit timeoutUnit = TimeUnit.SECONDS;

  /**
   * Constructor
   *
   * @param clientImpl implementation of client to handle request
   * @param method     http method to use for Request
   */
  protected EtcdRequest(EtcdClientImpl clientImpl, HttpMethod method) {
    this.clientImpl = clientImpl;
    this.method = method;
  }

  /**
   * Send request to etcd server
   *
   * @return Promise
   * @throws java.io.IOException if sending fails
   */
  public abstract EtcdResponsePromise<R> send() throws IOException;

  /**
   * Get Http Method
   *
   * @return HttpMethod
   */
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Get the URI to send request to
   *
   * @return String representing URI
   */
  public abstract String getUri();

  /**
   * Get the request parameters
   *
   * @return Map with key values as request parameters
   */
  public Map<String, String> getRequestParams() {
    return null;
  }

  /**
   * Set promise on request
   *
   * @param promise to set
   */
  public void setPromise(EtcdResponsePromise<R> promise) {
    this.promise = promise;
  }

  /**
   * Get promise for request
   *
   * @return promise
   */
  public EtcdResponsePromise<R> getPromise() {
    return promise;
  }

  /**
   * Get the timeout for request
   *
   * @return timeout in milliseconds
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * Set timeout for request
   *
   * @param timeout in unit
   * @param unit    time unit for timeout
   * @return Itself for chaining
   */
  public EtcdRequest timeout(int timeout, TimeUnit unit) {
    this.timeout = timeout;
    this.timeoutUnit = unit;
    return this;
  }

  /**
   * Get timeout unit
   *
   * @return Unit of timeout
   */
  public TimeUnit getTimeoutUnit() {
    return timeoutUnit;
  }
}