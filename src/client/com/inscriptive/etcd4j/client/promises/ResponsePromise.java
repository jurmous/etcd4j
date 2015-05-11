package com.inscriptive.etcd4j.client.promises;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import com.inscriptive.etcd4j.client.ConnectionState;
import com.inscriptive.etcd4j.client.retry.ConnectionFailHandler;
import com.inscriptive.etcd4j.client.retry.RetryHandler;
import com.inscriptive.etcd4j.client.retry.RetryPolicy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

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

  List<IsSimplePromiseResponseHandler<T>> handlers;
  private final GenericFutureListener<Promise<T>> promiseHandler;

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

    promiseHandler = new GenericFutureListener<Promise<T>>() {
      @Override
      public void operationComplete(Promise<T> future) throws Exception {
        handlePromise(future);
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
    if (handlers == null) {
      handlers = Collections.singletonList(listener);
    } else {
      handlers.add(listener);
    }
    if (response != null || exception != null) {
      listener.onResponse(this);
    }
  }

  /**
   * Remove a listener
   *
   * @param listener to remove
   */
  public void removeListener(IsSimplePromiseResponseHandler<T> listener) {
    if (handlers != null) {
      handlers.remove(listener);
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
      this.response = promise.getNow();
      if (handlers != null) {
        for (IsSimplePromiseResponseHandler<T> h : handlers) {
          h.onResponse(this);
        }
      }
    }
  }

  /**
   * Sets exception
   *
   * @param exception to set.
   */
  public void setException(Throwable exception) {
    this.exception = exception;

    if (handlers != null) {
      for (IsSimplePromiseResponseHandler<T> h : handlers) {
        h.onResponse(this);
      }
    }
  }

  /**
   * Get the response. (Blocking)
   * Use addListener to fetch the value in a non blocking way.
   *
   * @return the response
   * @throws Exception on fail
   */
  public T get() throws Exception {
    if (!waitForPromiseSuccess()) {
      return this.get();
    }

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
   * @return true if success, false if fail
   * @throws IOException      on IOException
   * @throws TimeoutException on timeout
   */
  protected boolean waitForPromiseSuccess() throws IOException, TimeoutException {
    if (!promise.isDone()) {
      Promise<T> listeningPromise = this.promise;
      listeningPromise.awaitUninterruptibly();
      if (listeningPromise != this.promise) {
        return false;
      }
      this.handlePromise(promise);
    }
    return true;
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
      this.retryPolicy.retry(connectionState, retryHandler, new ConnectionFailHandler() {
        @Override public void catchException(IOException exception) {
          handleRetry(exception);
        }
      });
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
