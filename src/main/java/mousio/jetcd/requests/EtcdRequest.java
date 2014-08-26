package mousio.jetcd.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.transport.EtcdClientImpl;

import java.io.IOException;
import java.util.Map;

/**
 * Request to Etcd
 *
 * @param <R> Response Type returned by request
 */
public abstract class EtcdRequest<R> {
  protected final EtcdClientImpl clientImpl;

  protected final HttpMethod method;
  private EtcdResponsePromise<R> promise;

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
}