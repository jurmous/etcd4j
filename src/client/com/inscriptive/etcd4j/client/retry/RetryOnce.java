package com.inscriptive.etcd4j.client.retry;

import com.inscriptive.etcd4j.client.ConnectionState;

/**
 * Retries once after a delay
 */
public class RetryOnce extends RetryPolicy {

  public boolean retryAttempted = false;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before retrying
   */
  public RetryOnce(int msBeforeRetry) {
    super(msBeforeRetry);
  }

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    if (!retryAttempted) {
      retryAttempted = true;
      return true;
    } else {
      return false;
    }
  }
}