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
package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.responses.EtcdVersionResponse;
import mousio.etcd4j.transport.EtcdClientImpl;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 *
 * An Etcd Version Request
 */
public class EtcdVersionRequest extends AbstractEtcdRequest<EtcdVersionResponse> {

  /**
   * Constructor
   *
   * @param clientImpl   the client to handle this request
   * @param retryHandler handles retries
   */
  public EtcdVersionRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
    super("/version", clientImpl, HttpMethod.GET, retryHandler, EtcdVersionResponse.DECODER);
  }

  @Override
  public EtcdVersionRequest setRetryPolicy(RetryPolicy retryPolicy) {
    super.setRetryPolicy(retryPolicy);
    return this;
  }
}