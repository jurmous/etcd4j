/*
 * Copyright (c) 2015, contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdResponseDecoder;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 *
 * Request to Etcd
 *
 * @param <R> Response Type returned by request
 */
public abstract class EtcdRequest<R> {
  protected final EtcdClientImpl clientImpl;

  protected final HttpMethod method;

  protected RetryPolicy retryPolicy;

  private EtcdResponsePromise<R> promise;
  private final EtcdResponseDecoder<R> decoder;

  private long timeout = -1L;
  private TimeUnit timeoutUnit = TimeUnit.SECONDS;
  private HttpRequest httpRequest;
  private String url;

  /**
   * Constructor
   *
   * @param clientImpl  implementation of client to handle request
   * @param method      http method to use for Request
   * @param retryPolicy Handles retries on fails
   * @param decoder     The response decoder
   */
  protected EtcdRequest(
      EtcdClientImpl clientImpl, HttpMethod method, RetryPolicy retryPolicy, EtcdResponseDecoder<R> decoder) {

    this.clientImpl = clientImpl;
    this.method = method;
    this.retryPolicy = retryPolicy;
    this.decoder = decoder;
  }

  /**
   * Send request to etcd server
   *
   * @return Promise
   * @throws java.io.IOException if sending fails
   */
  public abstract EtcdResponsePromise<R> send() throws IOException;

  /**
   *
   * @return
   */
  public EtcdResponseDecoder<R> getResponseDecoder() {
    return this.decoder;
  }

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
  public long getTimeout() {
    return timeout;
  }

  /**
   * Set timeout for request
   *
   * @param timeout in unit
   * @param unit    time unit for timeout
   * @return Itself for chaining
   */
  public EtcdRequest timeout(long timeout, TimeUnit unit) {
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

  /**
   * Set the used HTTPRequest
   * (For debugging and warnings)
   *
   * @param httpRequest set http request
   */
  public void setHttpRequest(HttpRequest httpRequest) {
    this.httpRequest = httpRequest;
  }

  /**
   * Get the used HTTP request
   *
   * @return http request
   */
  public HttpRequest getHttpRequest() {
    return httpRequest;
  }

  /**
   * Set a specific retry policy for this request
   *
   * @param retryPolicy to set for this request
   * @return self for chaining.
   */
  public EtcdRequest setRetryPolicy(RetryPolicy retryPolicy) {
    this.retryPolicy = retryPolicy;
    return this;
  }

  /**
   * Get the current Retry handler
   *
   * @return retry handler
   */
  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  /**
   * Get current URL of request
   *
   * @return current url
   */
  public String getUrl() {
    if (this.url != null) {
      return this.url;
    } else {
      return this.getUri();
    }
  }

  /**
   * Set current URL
   *
   * @param url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }
}