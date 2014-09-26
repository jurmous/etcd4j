package mousio.etcd4j.promises;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import mousio.etcd4j.responses.EtcdException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * A Promise for a response
 *
 * @param <T> Type of response contained
 */
public class EtcdResponsePromise<T> {
  protected Promise<T> promise;

  protected T response;
  protected Throwable exception;

  List<IsSimplePromiseResponseHandler<T>> handlers;
  private final GenericFutureListener<Promise<T>> promiseHandler = (GenericFutureListener<Promise<T>>) this::handlePromise;

  /**
   * Attach Netty Promise
   *
   * @param promise netty promise to set up response promise with
   */
  public void attachNettyPromise(Promise<T> promise) {
    promise.addListener(promiseHandler);
    if (this.promise != null) {
      this.promise.removeListener(promiseHandler);
      this.promise.cancel(true);
    }
    this.promise = promise;
  }

  /**
   * Add a promise to do when Response comes in
   *
   * @param listener to add
   */
  public void addListener(IsSimplePromiseResponseHandler<T> listener) {
    if (handlers == null) {
      handlers = Arrays.asList(listener);
    } else {
      handlers.add(listener);
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
   * <p>
   * Use addListener to fetch the value in a non blocking way.
   *
   * @return the response
   * @throws IOException      on fail (Will be ReadTimeoutException if timeout occurred)
   * @throws EtcdException    on etcd fail
   * @throws TimeoutException on Timeout
   */
  public T get() throws IOException, EtcdException, TimeoutException {
    if (!promise.isDone()) {
      Promise<T> listeningPromise = this.promise;
      listeningPromise.awaitUninterruptibly();
      if (listeningPromise != this.promise) {
        return this.get();
      }

      this.handlePromise(promise);
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
    public void onResponse(EtcdResponsePromise<T> response);
  }
}