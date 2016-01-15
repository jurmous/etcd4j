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
 * Retries with an exponential backoff
 */
public class RetryWithExponentialBackOff extends RetryPolicy {
  public static final RetryWithExponentialBackOff DEFAULT =  new RetryWithExponentialBackOff(20, -1, 10000);

  private final int maxRetryCount;
  private final int maxDelay;

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry) {
    this(startMsBeforeRetry, -1, -1);
  }

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   * @param maxRetryCount      max retry count
   * @param maxDelay           max delay between retries
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry, int maxRetryCount, int maxDelay) {
    super(startMsBeforeRetry);
    this.maxRetryCount = maxRetryCount;
    this.maxDelay = maxDelay;
  }

  @Override public boolean shouldRetry(ConnectionState state) {
    if (this.maxRetryCount != -1 && state.retryCount >= this.maxRetryCount) {
      return false;
    }

    if (state.msBeforeRetry == 0) {
      state.msBeforeRetry = this.startRetryTime;
    } else if (maxDelay == -1) {
      state.msBeforeRetry *= 2;
    } else if (state.msBeforeRetry < maxDelay) {
      state.msBeforeRetry *= 2;
      if (state.msBeforeRetry > maxDelay) {
        state.msBeforeRetry = maxDelay;
      }
    } else {
      return false;
    }

    return true;
  }
}