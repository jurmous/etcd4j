package mousio.etcd4j.requests;


import io.netty.handler.codec.http.HttpMethod;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.transport.EtcdClientImpl;

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
   * @param clientImpl the client to handle this request
   * @param method     to set request with
   */
  public EtcdKeyRequest(EtcdClientImpl clientImpl, HttpMethod method) {
    super(clientImpl, method);
  }

  /**
   * Set key for request
   *
   * @param key to do action on
   * @return EtcdKeysRequest for chaining
   */
  public EtcdKeyRequest setKey(String key) {
    this.key = key;
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