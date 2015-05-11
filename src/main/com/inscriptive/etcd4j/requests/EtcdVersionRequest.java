package com.inscriptive.etcd4j.requests;

import com.inscriptive.etcd4j.transport.EtcdClientImpl;
import io.netty.handler.codec.http.HttpMethod;
import com.inscriptive.etcd4j.client.retry.RetryPolicy;
import com.inscriptive.etcd4j.promises.EtcdResponsePromise;

import java.io.IOException;

/**
 * An Etcd Version Request
 */
public class EtcdVersionRequest extends EtcdRequest<String> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdVersionRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler);
  }

  @Override public EtcdResponsePromise<String> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public EtcdVersionRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public String getUri() {
    return "/version";
  }
}