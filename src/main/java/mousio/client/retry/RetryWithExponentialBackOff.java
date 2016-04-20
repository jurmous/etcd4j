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
  private final int maxDelayInMs;

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry) {
    this(startMsBeforeRetry, -1, 5000);
  }

  /**
   * Constructor
   *
   * @param startMsBeforeRetry milliseconds before retrying base time
   * @param maxRetryCount      max retry count, if maxRetryCount &lt;= 0, it will retry infinitely
   * @param maxDelayInMs           max delay between retries
   */
  public RetryWithExponentialBackOff(int startMsBeforeRetry, int maxRetryCount, int maxDelayInMs) {
    super(startMsBeforeRetry);
    this.maxRetryCount = maxRetryCount;
    this.maxDelayInMs = maxDelayInMs;

    if (startMsBeforeRetry <= 0) {
      throw new IllegalArgumentException("RetryWithExponentialBackOff.startMsBeforeRetry must be > 0!");
    }

    if (maxDelayInMs <= 0) {
      throw new IllegalArgumentException("RetryWithExponentialBackOff.maxDelay must be > 0!");
    }
  }

  @Override public boolean shouldRetry(ConnectionState state) {
    if (this.maxRetryCount > 0 && state.retryCount > this.maxRetryCount) {
      return false;
    }

    if (state.msBeforeRetry <= 0) {
      state.msBeforeRetry = startRetryTime;
    } else if (state.msBeforeRetry < maxDelayInMs) {
      state.msBeforeRetry *= 2;
    }

    if (state.msBeforeRetry > maxDelayInMs) {
      state.msBeforeRetry = maxDelayInMs;
    }

    return true;
  }
}
