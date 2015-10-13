/*
 * Copyright (c) 2015, contributors as indicated by the @author tags.
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
package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdSelfStatsResponse;
import mousio.etcd4j.responses.EtcdSelfStatsResponseDecoder;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 *
 * An Etcd Store Stats Request
 */
public class EtcdSelfStatsRequest extends EtcdRequest<EtcdSelfStatsResponse> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdSelfStatsRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
    super(clientImpl, HttpMethod.GET, retryHandler, EtcdSelfStatsResponseDecoder.INSTANCE);
  }

  @Override public EtcdResponsePromise<EtcdSelfStatsResponse> send() throws IOException {
    return clientImpl.send(this);
  }

  @Override public EtcdSelfStatsRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }

  @Override public String getUri() {
    return "/v2/stats/self";
  }
}