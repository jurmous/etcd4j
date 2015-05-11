package com.inscriptive.etcd4j.promises;

import com.inscriptive.etcd4j.responses.EtcdException;
import com.inscriptive.etcd4j.client.ConnectionState;
import com.inscriptive.etcd4j.client.promises.ResponsePromise;
import com.inscriptive.etcd4j.client.retry.RetryHandler;
import com.inscriptive.etcd4j.client.retry.RetryPolicy;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * A Promise for a response
 *
 * @param <T> Type of response contained
 */
public class EtcdResponsePromise<T> extends ResponsePromise<T> {

  /**
   * Constructor
   *
   * @param retryPolicy     the policy for retries
   * @param connectionState which contains current connection details
   * @param retryHandler    handler for retries
   */
  public EtcdResponsePromise(RetryPolicy retryPolicy, ConnectionState connectionState, RetryHandler retryHandler) {
    super(retryPolicy, connectionState, retryHandler);
  }

  /**
   * Get the response. (Blocking)
   * Use addListener to fetch the value in a non blocking way.
   *
   * @return the response
   * @throws IOException      on fail (Will be ReadTimeoutException if timeout occurred)
   * @throws EtcdException    on etcd fail
   * @throws TimeoutException on Timeout
   */
  @Override public T get() throws IOException, EtcdException, TimeoutException {
    if (!waitForPromiseSuccess()) {
      return this.get();
    }

    if (response != null) {
      return response;
    } else {
      if (this.exception instanceof EtcdException) {
        throw (EtcdException) this.exception;
      } else if (this.exception instanceof IOException) {
        throw (IOException) this.exception;
      } else if (this.exception instanceof io.netty.handler.timeout.TimeoutException) {
        throw new TimeoutException();
      } else {
        throw new IOException(this.exception);
      }
    }
  }
}