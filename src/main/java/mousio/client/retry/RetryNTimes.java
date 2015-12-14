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
 * Will retry the command N number of times
 */
public class RetryNTimes extends RetryPolicy {
  private final int timesToRetry;

  /**
   * Constructor
   *
   * @param msBeforeRetry milliseconds before retrying
   * @param timesToRetry  number of times to retry
   */
  public RetryNTimes(int msBeforeRetry, int timesToRetry) {
    super(msBeforeRetry);
    this.timesToRetry = timesToRetry;
  }

  @Override public boolean shouldRetry(ConnectionState connectionState) {
    return connectionState.retryCount < timesToRetry;
  }
}