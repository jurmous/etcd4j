package mousio.client.retry;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import mousio.client.ConnectionState;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Interface for handling a retry
 */
public abstract class RetryPolicy {
  private static final HashedWheelTimer timer = new HashedWheelTimer();

  protected int msBeforeRetry;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before trying retry
   */
  public RetryPolicy(int msBeforeRetry) {
    this.msBeforeRetry = msBeforeRetry;
  }

  /**
   * Does the retry. Will always try all URIs before throwing an exception.
   *
   * @param state        of connection
   * @param retryHandler handles the retry itself
   * @param failHandler  handles the fail
   */
  public void retry(final ConnectionState state, final RetryHandler retryHandler, final ConnectionFailHandler failHandler) {
    timer.newTimeout(new TimerTask() {
      @Override public void run(Timeout timeout) throws Exception {
        retryAllUris(state, retryHandler, failHandler);
      }
    }, this.getRetryTimeInMs(), TimeUnit.MILLISECONDS);
  }

  /**
   * Does the retry. Will always try all URIs before throwing an exception.
   *
   * @param state        of connection
   * @param retryHandler handles the retry itself
   * @param failHandler  handles the fail
   */
  private void retryAllUris(ConnectionState state, RetryHandler retryHandler, ConnectionFailHandler failHandler) {
    IOException ex = null;
    for (int i = 0; i < state.uris.length; i++) {
      URI uri = state.uris[i];
      try {
        state.uriIndex = i;
        retryHandler.doRetry();

        break;
      } catch (IOException e) {
        ex = e;
      }
    }
    if (ex != null) {
      failHandler.catchException(ex);
    }
  }

  /**
   * Get the retry time
   *
   * @return time in ms for retry
   */
  public int getRetryTimeInMs() {
    return msBeforeRetry;
  }

  /**
   * Should another retry be attempted according to the policy
   *
   * @param connectionState current connection state
   * @return true if retry should be attempted, false if not.
   */
  public abstract boolean shouldRetry(ConnectionState connectionState);
}