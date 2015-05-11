package com.inscriptive.etcd4j.client.retry;

import java.io.IOException;

/**
 * Interface to handle a retry
 */
public interface RetryHandler {
  /**
   * Do the retry
   *
   * @throws java.io.IOException if connection fails
   */
  public void doRetry() throws IOException;
}