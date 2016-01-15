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
package mousio.etcd4j.transport;

import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdRequest;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Jurriaan Mous
 *
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