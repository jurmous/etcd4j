/*
 * Copyright (c) 2015, Jurriaan Mous and contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mousio.etcd4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import io.netty.handler.ssl.SslContext;
import mousio.client.retry.RetryPolicy;
import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.requests.*;
import mousio.etcd4j.responses.*;
import mousio.etcd4j.transport.EtcdClientImpl;
import mousio.etcd4j.transport.EtcdNettyClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Etcd client.
 */
public class EtcdClient implements Closeable {
  private final EtcdClientImpl client;
  private RetryPolicy retryHandler;

  /**
   * Constructor
   *
   * @param baseUri URI to create connection on
   */
  public EtcdClient(URI... baseUri) {
    this(EtcdSecurityContext.NONE, baseUri);
  }

  /**
   * Constructor
   *
   * @param username  username
   * @param password  password
   * @param baseUri   URI to create connection on
   */
  public EtcdClient(String username, String password, URI... baseUri) {
    this(EtcdSecurityContext.withCredential(username, password), baseUri);
  }

  /**
   * Constructor
   *
   * @param sslContext  context for Ssl connections
   * @param username    username
   * @param password    password
   * @param baseUri     URI to create connection on
   */
  public EtcdClient(SslContext sslContext, String username, String password, URI... baseUri) {
    this(new EtcdSecurityContext(sslContext, username, password), baseUri);
  }

  /**
   * Constructor
   *
   * @param sslContext  context for Ssl connections
   * @param baseUri     URI to create connection on
   */
  public EtcdClient(SslContext sslContext, URI... baseUri) {
    this(EtcdSecurityContext.withSslContext(sslContext), baseUri);
  }

  /**
   * Constructor
   *
   * @param securityContext context for security
   * @param baseUri URI to create connection on
   */
  public EtcdClient(EtcdSecurityContext securityContext, URI... baseUri) {
    this(new EtcdNettyClient(
            securityContext,
            (baseUri.length == 0)
                    ? new URI[] { URI.create("https://127.0.0.1:4001") }
                    : baseUri
    ));
  }

  /**
   * Create a client with a custom implementation
   *
   * @param etcdClientImpl to create client with.
   */
  public EtcdClient(EtcdClientImpl etcdClientImpl) {
    this.client = etcdClientImpl;
    this.retryHandler = RetryWithExponentialBackOff.DEFAULT;
  }

  /**
   * Get the version of the Etcd server
   *
   * @return version as String
   * @deprecated use version() when using etcd 2.1+.
   */
  @Deprecated
  public String getVersion() {
    try {
      return new EtcdOldVersionRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the version of the Etcd server
   *
   * @return version
   */
  public EtcdVersionResponse version() {
    try {
      return new EtcdVersionRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the Self Statistics of Etcd
   *
   * @return EtcdSelfStatsResponse
   */
  public EtcdSelfStatsResponse getSelfStats() {
    try {
      return new EtcdSelfStatsRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the Leader Statistics of Etcd
   *
   * @return EtcdLeaderStatsResponse
   */
  public EtcdLeaderStatsResponse getLeaderStats() {
    try {
      return new EtcdLeaderStatsRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the Store Statistics of Etcd
   *
   * @return vEtcdStoreStatsResponse
   */
  public EtcdStoreStatsResponse getStoreStats() {
    try {
      return new EtcdStoreStatsRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the Members of Etcd
   *
   * @return vEtcdMembersResponse
   */
  public EtcdMembersResponse getMembers() {
    try {
      return new EtcdMembersRequest(this.client,retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Get the Members of Etcd
   *
   * @return vEtcdMembersResponse
   */
  public EtcdHealthResponse getHealth() {
    try {
      return new EtcdHealthRequest(this.client,retryHandler).send().get();
    } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Put a key with a value
   *
   * @param key to put
   * @param value to put on key
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest put(String key, String value) {
    return new EtcdKeyPutRequest(client, key, retryHandler).value(value);
  }

  /**
   * Refresh a key with new ttl
   * (without notifying watchers when using etcd 2.3+)
   *
   * @param key to refresh
   * @param ttl to update key with
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest refresh(String key, Integer ttl) {
    return new EtcdKeyPutRequest(client, key, retryHandler).refresh(ttl);
  }

  /**
   * Create a dir
   *
   * @param dir to create
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest putDir(String dir) {
    return new EtcdKeyPutRequest(client, dir, retryHandler).isDir();
  }

  /**
   * Post a value to a key for in-order keys.
   *
   * @param key to post to
   * @param value to post
   * @return EtcdKeysRequest
   */
  public EtcdKeyPostRequest post(String key, String value) {
    return new EtcdKeyPostRequest(client, key, retryHandler).value(value);
  }

  /**
   * Deletes a key
   *
   * @param key to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyDeleteRequest delete(String key) {
    return new EtcdKeyDeleteRequest(client, key, retryHandler);
  }

  /**
   * Deletes a directory
   *
   * @param dir to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyDeleteRequest deleteDir(String dir) {
    return new EtcdKeyDeleteRequest(client, dir, retryHandler).dir();
  }

  /**
   * Get by key
   *
   * @param key to get
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest get(String key) {
    return new EtcdKeyGetRequest(client, key, retryHandler);
  }

  /**
   * Gets the content of the key recursively as a JsonObject
   * @param path root path (i.e. /path1/path2)
   * @return JsonNode
   */
  public JsonNode getAsJson(String path) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    EtcdKeyGetRequest etcdKeyGetRequest = new EtcdKeyGetRequest(client, path, retryHandler).recursive();
    EtcdKeysResponse dataTree = etcdKeyGetRequest.send().get();

    ObjectNode jNode = JsonNodeFactory.instance.objectNode();
    dataTree.getNode().getNodes().forEach(node->iterateOverNodes(jNode, node));

    return jNode.at(path);
  }

  /**
   * Puts the content of the Json recursively from the specified <i>path</i>
   * @param path root path (i.e. /path1/path2)
   * @param data JsonNode
   */
  public void putAsJson(String path, JsonNode data) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    Map<String, Object> flattened = new JsonFlattener(EtcdUtil.printJson(data))
            .withFlattenMode(FlattenMode.NORMAL)
            .withSeparator('/')
            .flattenAsMap();

    for (Map.Entry<String, Object> entry : flattened.entrySet()) {
      put(path + "/" + entry.getKey(), String.valueOf(entry.getValue())).send().get();
    }
  }

  /**
   * Iterates recursively over every node path, translating the structure of the node to a JSON structure on the parameter <i>json</i>
   * @param json JSON object to complete using the node information
   * @param node Etcd node information (recursively obtained)
   */
  private void iterateOverNodes(ObjectNode json, EtcdKeysResponse.EtcdNode node) {
    if (node.isDir()) {
      node.getNodes().forEach(n->iterateOverNodes(json, n));
    } else {
      List<String> keyPath = Arrays.stream(node.getKey().split("/"))
              .filter(n->!n.isEmpty())
              .collect(Collectors.toList());

      ObjectNode jsonNodes = json;
      Iterator<String> iterator = keyPath.iterator();

      while (iterator.hasNext()) {
        String key = iterator.next();
        if (jsonNodes.get(key)==null) {
          if (iterator.hasNext()) {
            jsonNodes = jsonNodes.putObject(key);
          } else {
            jsonNodes = jsonNodes.put(key, node.getValue());
          }
        } else {
          jsonNodes = (ObjectNode) jsonNodes.get(key);
        }
      }
    }
  }

  /**
   * Get directory
   *
   * @param dir to get
   * @return EtcdKeysGetRequest
   */
  public EtcdKeyGetRequest getDir(String dir) {
    return new EtcdKeyGetRequest(client, dir, retryHandler).dir();
  }

  /**
   * Get all keys
   *
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest getAll() {
    return new EtcdKeyGetRequest(client, retryHandler);
  }

  @Override
  public void close() throws IOException {
    if (client != null) {
      client.close();
    }
  }

  /**
   * Set the retry handler. Default is an exponential back-off with start of 20ms.
   *
   * @param retryHandler to set
   * @return this instance
   */
  public EtcdClient setRetryHandler(RetryPolicy retryHandler) {
    this.retryHandler = retryHandler;
    return this;
  }
}
