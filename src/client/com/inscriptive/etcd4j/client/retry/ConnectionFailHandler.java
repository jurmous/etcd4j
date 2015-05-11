package com.inscriptive.etcd4j.client.retry;

import java.io.IOException;

/**
 * Interface to handle a retry
 */
public interface ConnectionFailHandler {
  /**
   * Catch exception on retry
   *
   * @param exception to be handled
   */
  public void catchException(IOException exception);
}