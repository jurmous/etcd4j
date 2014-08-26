package mousio.jetcd.promises;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import mousio.jetcd.responses.EtcdException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A Promise for a response
 *
 * @param <T> Type of response contained
 */
public class EtcdResponsePromise<T> {
  protected Promise<T> promise;
  protected T response;
  protected IOException ioException;
  protected EtcdException etcdException;

  List<IsSimplePromiseResponseHandler<T>> handlers;

  /**
   * @param promise netty promise to set up promise with
   */
  public void attachNettyPromise(Promise<T> promise) {
    promise.addListener((GenericFutureListener<Promise<T>>) this::handlePromise);
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
    if (exception instanceof EtcdException) {
      this.etcdException = (EtcdException) exception;
      return;
    } else if (exception instanceof IOException) {
      this.ioException = (IOException) exception;
    } else {
      this.ioException = new IOException(exception);
    }

    if (handlers != null) {
      for (IsSimplePromiseResponseHandler<T> h : handlers) {
        h.onResponse(this);
      }
    }
  }

  /**
   * Get the response
   *
   * @return the response
   * @throws IOException   on fail
   * @throws EtcdException on etcd fail
   */
  public T get() throws IOException, EtcdException {
    if (!promise.isDone()) {
      promise.awaitUninterruptibly();

      this.handlePromise(promise);
    }

    if (response != null) {
      return response;
    } else if (this.etcdException != null) {
      throw this.etcdException;
    } else {
      throw this.ioException;
    }
  }

  /**
   * Get the result now even if it is not loaded
   *
   * @return the result
   */
  public T getNow() {
    return response;
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