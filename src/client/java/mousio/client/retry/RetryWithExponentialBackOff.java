package mousio.client.retry;

import mousio.client.ConnectionState;

/**
 * Retries with an exponential backoff
 */
public class RetryWithExponentialBackOff extends RetryPolicy {
  private final int maxRetryCount;
  private final int maxDelay;

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry) {
    this(startMsBeforeRetry, -1, -1);
  }

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   * @param maxRetryCount      max retry count
   * @param maxDelay           max delay between retries
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry, int maxRetryCount, int maxDelay) {
    super(startMsBeforeRetry);
    this.maxRetryCount = maxRetryCount;
    this.maxDelay = maxDelay;
  }

  @Override public boolean shouldRetry(ConnectionState state) {
    if (this.maxRetryCount != -1 && state.retryCount >= this.maxRetryCount) {
      return false;
    }

    if (state.msBeforeRetry == 0) {
      state.msBeforeRetry = this.startRetryTime;
    } else if (maxDelay == -1) {
      state.msBeforeRetry *= 2;
    } else if (state.msBeforeRetry < maxDelay) {
      state.msBeforeRetry *= 2;
      if (state.msBeforeRetry > maxDelay) {
        state.msBeforeRetry = maxDelay;
      }
    } else {
      return false;
    }

    return true;
  }
}