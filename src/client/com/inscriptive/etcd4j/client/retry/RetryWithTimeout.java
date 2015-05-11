package com.inscriptive.etcd4j.client.retry;

import com.inscriptive.etcd4j.client.ConnectionState;

import java.util.Date;

/**
 * Retries with a delay until a timeout
 */
public class RetryWithTimeout extends RetryPolicy {
  private final long timeoutInMs;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before retrying
   * @param timeoutInMs   timeout in ms
   */
  public RetryWithTimeout(int msBeforeRetry, int timeoutInMs) {
    super(msBeforeRetry);
    this.timeoutInMs = timeoutInMs;
  }

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    return (new Date().getTime() - connectionState.startTime) < timeoutInMs;
  }
}