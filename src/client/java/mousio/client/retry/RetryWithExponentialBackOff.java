package mousio.client.retry;

import mousio.client.ConnectionState;

/**
 * Retries with an exponential backoff
 */
public class RetryWithExponentialBackOff extends RetryPolicy {
  private final int maxRetryCount;
  private final int maxDelay;

  protected int currentDelay = 0;

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry) {
    this(startMsBeforeRetry, -1, -1);
  }

  @Override public int getRetryTimeInMs() {
    return this.currentDelay;
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

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    if (this.maxRetryCount != -1 && connectionState.retryCount >= this.maxRetryCount) {
      return false;
    }

    if (currentDelay == 0) {
      currentDelay = this.msBeforeRetry;
    } else if (maxDelay == -1) {
      currentDelay *= 2;
    } else if (currentDelay < maxDelay) {
      currentDelay *= 2;
      if (currentDelay > maxDelay) {
        currentDelay = maxDelay;
      }
    }

    return true;
  }
}