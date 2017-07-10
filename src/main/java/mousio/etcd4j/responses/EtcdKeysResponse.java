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
package mousio.etcd4j.responses;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.handler.codec.http.HttpHeaders;

import static mousio.etcd4j.EtcdUtil.convertDate;
import static mousio.etcd4j.EtcdUtil.getHeaderPropertyAsLong;

/**
 * @author Luca Burgazzoli
 *
 * Etcd Keys Response
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public final class EtcdKeysResponse implements EtcdHeaderAwareResponse {

  // The json
  public static final EtcdResponseDecoder<EtcdKeysResponse> DECODER =
    EtcdResponseDecoders.json(EtcdKeysResponse.class);

  public final EtcdKeyAction action;
  public final EtcdNode node;
  public final EtcdNode prevNode;

  public String etcdClusterId;
  public Long etcdIndex;
  public Long raftIndex;
  public Long raftTerm;

  /**
   * Protected constructor
   *
   * @param action
   * @param node
   * @param prevNode
   */
  EtcdKeysResponse(
      @JsonProperty("action") String action,
      @JsonProperty("node") EtcdNode node,
      @JsonProperty("prevNode") EtcdNode prevNode) {

    this.action = EtcdKeyAction.valueOf(action);
    this.node = node;
    this.prevNode = prevNode;

    this.etcdClusterId = null;
    this.etcdIndex = null;
    this.raftIndex = null;
    this.raftTerm = null;
  }

  @Override
  public void loadHeaders(HttpHeaders headers) {
    if(headers != null) {
      this.etcdClusterId = headers.get("X-Etcd-Cluster-Id");
      this.etcdIndex = getHeaderPropertyAsLong(headers, "X-Etcd-Index");
      this.raftIndex = getHeaderPropertyAsLong(headers, "X-Raft-Index");
      this.raftTerm = getHeaderPropertyAsLong(headers, "X-Raft-Term");
    }
  }

  public EtcdKeyAction getAction() {
    return action;
  }

  public EtcdNode getNode() {
    return node;
  }

  public EtcdNode getPrevNode() {
    return prevNode;
  }

  /**
   * An Etcd node
   */
  @JsonIgnoreProperties( ignoreUnknown = true )
  public static final class EtcdNode {
    public final String key;
    public final boolean dir;
    public final Long createdIndex;
    public final Long modifiedIndex;
    public final String value;
    public final Date expiration;
    public final Long ttl;
    public final List<EtcdNode> nodes;

    /**
     * Etcd Node
     *
     * @param dir
     * @param key
     * @param value
     * @param createdIndex
     * @param modifiedIndex
     * @param expiration
     * @param ttl
     * @param nodes
     */
    EtcdNode(
        @JsonProperty("dir") final Boolean dir,
        @JsonProperty("key") final String key,
        @JsonProperty("value") final String value,
        @JsonProperty("createdIndex") final long createdIndex,
        @JsonProperty("modifiedIndex") final long modifiedIndex,
        @JsonProperty("expiration") final String expiration,
        @JsonProperty("ttl") final long ttl,
        @JsonProperty("nodes") final List<EtcdNode> nodes) {

      this.dir = dir != null ? dir : false;
      this.key = key;
      this.value = value;
      this.createdIndex = createdIndex;
      this.modifiedIndex = modifiedIndex;
      this.ttl = ttl;
      this.nodes = nodes != null
          ? Collections.unmodifiableList(nodes)
          : Collections.unmodifiableList(Collections.<EtcdNode>emptyList());
      this.expiration = expiration != null
          ? convertDate(expiration)
          : null;

   }
   public String getKey() {
     return key;
   }

   public boolean isDir() {
     return dir;
   }

   public Long getCreatedIndex() {
     return createdIndex;
   }

   public Long getModifiedIndex() {
     return modifiedIndex;
   }
   
   public String getValue() {
     return value;
   }

   public Date getExpiration() {
     return expiration;
   }

   public Long getTTL() {
     return ttl;
   }

   public List<EtcdNode> getNodes() {
     return nodes;
   }


      @Override
      public String toString() {
          return "EtcdNode{" +
              "key='" + key + '\'' +
              ", value='" + value + '\'' +
              ", dir=" + dir +
              ", createdIndex=" + createdIndex +
              ", modifiedIndex=" + modifiedIndex +
              ", expiration=" + expiration +
              ", ttl=" + ttl +
              ", nodes=" + nodes +
              '}';
      }
  }
}
