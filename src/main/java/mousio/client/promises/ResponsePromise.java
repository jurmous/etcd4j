/*
 * Copyright (c) 2015, Jurriaan Mous and contributors as indicated by the @author tags.
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
package mousio.client.promises;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import mousio.client.ConnectionState;
import mousio.client.retry.ConnectionFailHandler;
import mousio.client.retry.RetryHandler;
import mousio.client.retry.RetryPolicy;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Response promise
 *
 * @param <T> Type of object returned by promise
 */
public class ResponsePromise<T> {
  private final RetryPolicy retryPolicy;
  private final ConnectionState connectionState;
  private final RetryHandler retryHandler;
  protected Promise<T> promise;

  protected T response;
  protected Throwable exception;

  private List<IsSimplePromiseResponseHandler<T>> handlers;
  private final GenericFutureListener<Promise<T>> promiseHandler;
  private final ConnectionFailHandler connectionFailHandler;
  private final Lock lock;

  /**
   * Constructor
   *
   * @param retryPolicy     the policy for retries
   * @param connectionState which contains current connection details
   * @param retryHandler    handler for retries
   */
  public ResponsePromise(RetryPolicy retryPolicy, ConnectionState connectionState, RetryHandler retryHandler) {
    this.connectionState = connectionState;
    this.retryHandler = retryHandler;
    this.retryPolicy = retryPolicy;
    // add a lock to deal with concurrent operations for handlers
    this.lock = new ReentrantLock();

    promiseHandler = new GenericFutureListener<Promise<T>>() {
      @Override
      public void operationComplete(Promise<T> future) throws Exception {
        handlePromise(future);
      }
    };

    this.connectionFailHandler = new ConnectionFailHandler() {
      @Override
      public void catchException(IOException exception) {
        handleRetry(exception);
      }
    };
  }

  /**
   * Attach Netty Promise
   *
   * @param promise netty promise to set up response promise with
   */
  public void attachNettyPromise(Promise<T> promise) {
    promise.addListener(promiseHandler);
    Promise<T> oldPromise = this.promise;

    this.promise = promise;

    if (oldPromise != null) {
      oldPromise.removeListener(promiseHandler);
      oldPromise.cancel(true);
    }
  }

  /**
   * Add a promise to do when Response comes in
   *
   * @param listener to add
   */
  public void addListener(IsSimplePromiseResponseHandler<T> listener) {

    if (listener == null) {
      return;
    }

    boolean shouldInvoke = false;

    try {
      lock.lock();

      if (handlers == null) {
        handlers = new LinkedList<>();
      }

      handlers.add(listener);

      if (response != null || exception != null) {
        shouldInvoke = true;
      }

    } finally {
      lock.unlock();
    }

    if (shouldInvoke) {
      listener.onResponse(this);
    }
  }

  /**
   * Remove a listener
   *
   * @param listener to remove
   */
  public void removeListener(IsSimplePromiseResponseHandler<T> listener) {
    try {
      lock.lock();

      if (handlers != null) {
        handlers.remove(listener);
      }

    } finally {
      lock.unlock();
    }
  }

  /**
   * Handle the promise
   *
   * @param promise to handle
   */
  protected void handlePromise(Promise<T> promise) {

    if (!promise.isSuccess()) {
      this.setException(promise.cause());
    } else {

      List<IsSimplePromiseResponseHandler<T>> copy = null;

      try {
        lock.lock();
        this.response = promise.getNow();
        if (handlers != null) {
          copy = new LinkedList<>(handlers);
        }
      } finally {
        lock.unlock();
      }

      for (IsSimplePromiseResponseHandler<T> h : copy) {
        h.onResponse(this);
      }
    }
  }

  /**
   * Sets exception
   *
   * @param exception to set.
   */
  public void setException(Throwable exception) {

    List<IsSimplePromiseResponseHandler<T>> copy = null;

    try {
      lock.lock();
      this.exception = exception;
      if (handlers != null) {
        copy = new LinkedList<>(handlers);
      }
    } finally {
      lock.unlock();
    }

    if (copy != null) {
      for (IsSimplePromiseResponseHandler<T> h : copy) {
        h.onResponse(this);
      }
    }
  }

  /**
   * Get the response. (Blocking)
   *
   * Use addListener to fetch the value in a non blocking way.
   *
   * @return the response
   * @throws Exception on fail
   */
  public T get() throws Exception {
    waitForPromiseSuccess();

    if (response != null) {
      return response;
    } else {
      if (this.exception instanceof IOException) {
        throw (IOException) this.exception;
      } else if (this.exception instanceof io.netty.handler.timeout.TimeoutException) {
        throw new TimeoutException();
      } else {
        throw new IOException(this.exception);
      }
    }
  }

  /**
   * Wait for promise to be done
   *
   * @throws IOException      on IOException
   * @throws TimeoutException on timeout
   */
  protected void waitForPromiseSuccess() throws IOException, TimeoutException {
    while(!promise.isDone() && !promise.isCancelled()) {
      Promise<T> listeningPromise = this.promise;
      listeningPromise.awaitUninterruptibly();
      if (listeningPromise == this.promise) {
        this.handlePromise(promise);
        break;
      }
    }
  }

  /**
   * Get the result now even if it is not loaded yet by the promise.
   * Use get() to ensure in a blocking way that the value is loaded.
   *
   * @return the result
   */
  public T getNow() {
    return response;
  }

  /**
   * Get internal Netty Promise
   *
   * @return Netty Promise
   */
  public Promise<T> getNettyPromise() {
    return promise;
  }

  /**
   * Handles a retry
   *
   * @param cause of last connect fail
   */
  public void handleRetry(Throwable cause) {
    try {
      this.retryPolicy.retry(connectionState, retryHandler, connectionFailHandler);
    } catch (RetryPolicy.RetryCancelled retryCancelled) {
      this.getNettyPromise().setFailure(cause);
    }
  }

  /**
   * Get the current connection state
   *
   * @return connection state
   */
  public ConnectionState getConnectionState() {
    return connectionState;
  }

  /**
   * Cancel the request
   */
  public void cancel() {
    promise.cancel(true);
  }

  /**
   * Cancel the request
   *
   * @param throwable the exception to be associated to this promise
   */
  public void cancel(Throwable throwable) {
    this.promise.cancel(true);
    this.exception = throwable;
  }

  /**
   * Get exception
   *
   * @return the exception if set. Or null if no exception present
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * Response listener
   *
   * @param <T> Type contained
   */
  public interface IsSimplePromiseResponseHandler<T> {
    /**
     * Fired on response
     *
     * @param response with result
     */
    void onResponse(ResponsePromise<T> response);
  }
}
