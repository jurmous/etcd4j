package mousio.client.retry;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import mousio.client.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Interface for handling a retry
 */
public abstract class RetryPolicy {
  private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);
  private static final HashedWheelTimer timer = new HashedWheelTimer();

  protected final int startRetryTime;

  /**
   * Constructor
   *
   * @param startRetryTime start milliseconds before trying retry
   */
  public RetryPolicy(int startRetryTime) {
    this.startRetryTime = startRetryTime;
  }

  /**
   * Does the retry. Will always try all URIs before throwing an exception.
   *
   * @param state        of connection
   * @param retryHandler handles the retry itself
   * @param failHandler  handles the fail
   * @throws RetryCancelled if retry is cancelled
   */
  public final void retry(final ConnectionState state, final RetryHandler retryHandler, final ConnectionFailHandler failHandler) throws RetryCancelled {
    if (state.retryCount == 0) {
      state.msBeforeRetry = this.startRetryTime;
    }

    state.uriIndex++;
    if (state.uriIndex >= state.uris.length) {
      if (this.shouldRetry(state)) {
        logger.debug(String.format("Retry %s to send command", state.retryCount));
        state.retryCount += 1;
        state.uriIndex = 0;
        timer.newTimeout(new TimerTask() {
          @Override public void run(Timeout timeout) throws Exception {
            try {
              retryHandler.doRetry();
            } catch (IOException e) {
              failHandler.catchException(e);
            }
          }
        }, state.msBeforeRetry, TimeUnit.MILLISECONDS);
      } else {
        throw new RetryCancelled();
      }
    } else {
      try {
        retryHandler.doRetry();
      } catch (IOException e) {
        failHandler.catchException(e);
      }
    }
  }

  /**
   * Cancelled retry exception
   */
  public static class RetryCancelled extends Exception {
    private static final long serialVersionUID = 8043829471264975062L;
  }

  /**
   * Should another retry be attempted according to the policy
   *
   * @param connectionState current connection state
   * @return true if retry should be attempted, false if not.
   */
  public abstract boolean shouldRetry(ConnectionState connectionState);
}