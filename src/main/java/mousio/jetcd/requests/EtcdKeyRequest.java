package mousio.jetcd.requests;


import io.netty.handler.codec.http.HttpMethod;
import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.responses.EtcdKeysResponse;
import mousio.jetcd.transport.EtcdClientImpl;

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