package com.inscriptive.etcd4j.requests;


import com.inscriptive.etcd4j.promises.EtcdResponsePromise;
import com.inscriptive.etcd4j.responses.EtcdKeysResponse;
import com.inscriptive.etcd4j.transport.EtcdClientImpl;
import io.netty.handler.codec.http.HttpMethod;
import com.inscriptive.etcd4j.client.retry.RetryPolicy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic Etcd Keys Request
 */
public class EtcdKeyRequest extends EtcdRequest<EtcdKeysResponse> {
  protected String key;

  protected final Map<String, String> requestParams = new HashMap<>();

  /**
   * Constructs an EtcdKeysRequest
   *
   * @param clientImpl   the client to handle this request
   * @param method       to set request with
   * @param retryHandler Handles retries on fails
   */
  public EtcdKeyRequest(EtcdClientImpl clientImpl, HttpMethod method, RetryPolicy retryHandler) {
    super(clientImpl, method, retryHandler);
  }

  /**
   * Set key for request
   *
   * @param key to do action on
   * @return EtcdKeysRequest for chaining
   */
  public EtcdKeyRequest setKey(String key) {
    if (key.startsWith("/")){
      key = key.substring(1);
    }
    this.key = key;
    return this;
  }

  @Override public EtcdKeyRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public EtcdResponsePromise<EtcdKeysResponse> send() throws IOException {
    return this.clientImpl.send(this);
  }

  @Override public String getUri() {
    return "/v2/keys/" + ((key != null) ? key : "");
  }

  @Override public Map<String, String> getRequestParams() {
    return requestParams;
  }
}