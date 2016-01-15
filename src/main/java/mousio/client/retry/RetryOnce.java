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
package mousio.client.retry;

import mousio.client.ConnectionState;

/**
 * Retries once after a delay
 */
public class RetryOnce extends RetryPolicy {

  public boolean retryAttempted = false;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before retrying
   */
  public RetryOnce(int msBeforeRetry) {
    super(msBeforeRetry);
  }

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    if (!retryAttempted) {
      retryAttempted = true;
      return true;
    } else {
      return false;
    }
  }
}