package com.inscriptive.etcd4j.client;

import java.net.URI;

/**
 * Counts connection retries and current connection index
 */
public class ConnectionState {
  public int retryCount;
  public long startTime;
  public final URI[] uris;
  public int uriIndex;
  public int msBeforeRetry = 0;

  /**
   * Constructor
   *
   * @param uris to connect to
   */
  public ConnectionState(URI[] uris) {
    this.uris = uris;
  }
}
