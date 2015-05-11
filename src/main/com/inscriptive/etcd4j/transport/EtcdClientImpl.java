package com.inscriptive.etcd4j.transport;

import com.inscriptive.etcd4j.requests.EtcdRequest;
import com.inscriptive.etcd4j.promises.EtcdResponsePromise;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for Etcd client implementations
 */
public interface EtcdClientImpl extends Closeable {

  /**
   * Sends a request to the server
   *
   * @param request to send
   * @param <R>     Type of response
   * @return A Promise
   * @throws java.io.IOException if IO failure while sending
   */
  public <R> EtcdResponsePromise<R> send(EtcdRequest<R> request) throws IOException;

  @Override
  public void close();
}