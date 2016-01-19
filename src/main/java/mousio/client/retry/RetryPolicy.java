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

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import mousio.client.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Interface for handling a retry
 */
public abstract class RetryPolicy {
  private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);
  private static final HashedWheelTimer timer = new HashedWheelTimer();

  protected final int startRetryTime;

  /**
   * Constructor
   *
   * @param startRetryTime start milliseconds before trying retry
   */
  public RetryPolicy(int startRetryTime) {
    this.startRetryTime = startRetryTime;
  }

  /**
   * Does the retry. Will always try all URIs before throwing an exception.
   *
   * @param state        of connection
   * @param retryHandler handles the retry itself
   * @param failHandler  handles the fail
   * @throws RetryCancelled if retry is cancelled
   */
  public final void retry(final ConnectionState state, final RetryHandler retryHandler, final ConnectionFailHandler failHandler) throws RetryCancelled {
    if (state.retryCount == 0) {
      state.msBeforeRetry = this.startRetryTime;
    }

    state.retryCount++;
    state.uriIndex = state.retryCount % state.uris.length;

    if (this.shouldRetry(state)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Retry {} to send command", state.retryCount);
      }

      if(state.msBeforeRetry > 0) {
        timer.newTimeout(new TimerTask() {
          @Override
          public void run(Timeout timeout) throws Exception {
            try {
              retryHandler.doRetry(state);
            } catch (IOException e) {
              failHandler.catchException(e);
            }
          }
        }, state.msBeforeRetry, TimeUnit.MILLISECONDS);
      } else {
        try {
          retryHandler.doRetry(state);
        } catch (IOException e) {
          failHandler.catchException(e);
        }
      }
    } else {
      throw new RetryCancelled();
    }
  }

  /**
   * Cancelled retry exception
   */
  public static class RetryCancelled extends Exception {
    private static final long serialVersionUID = 8043829471264975062L;
  }

  /**
   * Should another retry be attempted according to the policy
   *
   * @param connectionState current connection state
   * @return true if retry should be attempted, false if not.
   */
  public abstract boolean shouldRetry(ConnectionState connectionState);
}