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
package mousio.etcd4j.promises;

import mousio.client.ConnectionState;
import mousio.client.promises.ResponsePromise;
import mousio.client.retry.RetryHandler;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * A Promise for a response
 *
 * @param <T> Type of response contained
 */
public class EtcdResponsePromise<T> extends ResponsePromise<T> {

  /**
   * Constructor
   *
   * @param retryPolicy     the policy for retries
   * @param connectionState which contains current connection details
   * @param retryHandler    handler for retries
   */
  public EtcdResponsePromise(RetryPolicy retryPolicy, ConnectionState connectionState, RetryHandler retryHandler) {
    super(retryPolicy, connectionState, retryHandler);
  }

  /**
   * Get the response. (Blocking)
   *
   * Use addListener to fetch the value in a non blocking way.
   *
   * @return the response
   * @throws IOException                  on fail (Will be ReadTimeoutException if timeout occurred)
   * @throws EtcdException                on etcd fail
   * @throws EtcdAuthenticationException  on authentication failure
   * @throws TimeoutException             on Timeout
   */
  @Override public T get() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    waitForPromiseSuccess();

    if (response != null) {
      return response;
    } else {
      if (this.exception instanceof EtcdException) {
        throw (EtcdException) this.exception;
      } if (this.exception instanceof EtcdAuthenticationException) {
        throw (EtcdAuthenticationException) this.exception;
      } else if (this.exception instanceof IOException) {
        throw (IOException) this.exception;
      } else if (this.exception instanceof io.netty.handler.timeout.TimeoutException) {
        throw new TimeoutException();
      } else {
        throw new IOException(this.exception);
      }
    }
  }
}