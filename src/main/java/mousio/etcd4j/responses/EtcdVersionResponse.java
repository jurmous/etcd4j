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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Luca Burgazzoli
 *
 * Etcd Keys Response
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public final class EtcdVersionResponse implements EtcdResponse {
  // The json
  public static final EtcdResponseDecoder<EtcdVersionResponse> DECODER =
    EtcdResponseDecoders.json(EtcdVersionResponse.class);

  public final String server;
  public final String cluster;

  /**
   * Protected constructor
   *
   * @param server
   * @param cluster
   */
  EtcdVersionResponse(
    @JsonProperty("etcdserver") String server,
    @JsonProperty("etcdcluster")  String cluster) {
    this.server = server;
    this.cluster = cluster;
  }
}