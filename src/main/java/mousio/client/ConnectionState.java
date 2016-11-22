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
package mousio.client;

import io.netty.channel.EventLoop;

import java.net.URI;

/**
 * Counts connection retries and current connection index
 */
public class ConnectionState {
  public EventLoop loop;
  public final URI[] uris;

  public int retryCount;
  public volatile int uriIndex;
  public int msBeforeRetry;
  public long startTime;

  /**
   * Constructor
   *
   * @param uris to connect to
   * @param uriIndex the uri index to start from
   */
  public ConnectionState(URI[] uris, int uriIndex) {
    this.loop = null;
    this.uris = uris;
    this.uriIndex = uriIndex;
    this.retryCount = 0;
    this.msBeforeRetry = 0;
    this.startTime = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("ConnectionState [")
      .append("\nretryCount=").append(retryCount).append(", ")
      .append("\nuriIndex=").append(uriIndex).append(", ")
      .append("\nmsBeforeRetry=").append(msBeforeRetry).append(", ")
      .append("\nstartTime=").append(startTime);

    if(uris != null) {
      builder.append("\nuris={");
      for(int i=0; i< uris.length; i++) {
        builder.append("\n\t").append(i).append("=").append(uris[i].toASCIIString());
      }
    }

    return builder.append("\n]").toString();
  }
}
