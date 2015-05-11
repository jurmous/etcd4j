package com.inscriptive.etcd4j.client.retry;

import com.inscriptive.etcd4j.client.ConnectionState;

/**
 * Will retry the command N number of times
 */
public class RetryNTimes extends RetryPolicy {
  private final int timesToRetry;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before retrying
   * @param timesToRetry  number of times to retry
   */
  public RetryNTimes(int msBeforeRetry, int timesToRetry) {
    super(msBeforeRetry);
    this.timesToRetry = timesToRetry;
  }

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    return connectionState.retryCount < timesToRetry;
  }
}