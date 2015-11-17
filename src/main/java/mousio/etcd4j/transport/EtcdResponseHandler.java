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
package mousio.etcd4j.transport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Promise;
import mousio.client.exceptions.PrematureDisconnectException;
import mousio.etcd4j.requests.EtcdRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 *
 * Handles etcd responses
 *
 * @param <R> Response type
 */
class EtcdResponseHandler<R> extends SimpleChannelInboundHandler<FullHttpResponse> {
  private static final Logger logger = LoggerFactory.getLogger(EtcdResponseHandler.class);

  private static final CharSequence HTTP_HEADER_LOCATION = "Location";

  protected final Promise<R> promise;
  protected final EtcdNettyClient client;
  protected final EtcdRequest<R> request;

  private boolean isRetried;

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdRequest     request
   */
  @SuppressWarnings("unchecked")
  public EtcdResponseHandler(EtcdNettyClient etcdNettyClient, EtcdRequest<R> etcdRequest) {
    this.client = etcdNettyClient;
    this.request = etcdRequest;
    this.promise = etcdRequest.getPromise().getNettyPromise();
    this.isRetried = false;
  }

  /**
   * Set if the connection is retried.
   * If true the promise will not fail on unregistering this handler.
   *
   * @param retried true if request is being retried.
   */
  public void retried(boolean retried) {
    this.isRetried = retried;
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!isRetried && !promise.isDone()) {
      this.request.getPromise().handleRetry(new PrematureDisconnectException());
    }
    super.channelUnregistered(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug(
        "Received " + response.status().code() + " for " + this.request.getMethod().name() + " "
          + this.request.getUri());
    }

    if (response.status().equals(HttpResponseStatus.MOVED_PERMANENTLY)
      || response.status().equals(HttpResponseStatus.TEMPORARY_REDIRECT)) {
      if (response.headers().contains(HTTP_HEADER_LOCATION)) {
        this.request.setUrl(response.headers().get(HTTP_HEADER_LOCATION));
        this.client.connect(this.request);
        // Closing the connection which handled the previous request.
        ctx.close();
        if (logger.isDebugEnabled()) {
          logger.debug(
            "redirect for " + this.request.getHttpRequest().uri() + " to " + response.headers()
              .get(HTTP_HEADER_LOCATION));
        }
      } else {
        this.promise.setFailure(new Exception("Missing Location header on redirect"));
      }
    } if(response.status().equals(HttpResponseStatus.UNAUTHORIZED)) {
      this.promise.setFailure(new EtcdAuthenticationException(response.content().toString(CharsetUtil.UTF_8)));
    } else {
      if (!response.content().isReadable()) {
        // If connection was accepted maybe response has to be waited for
        if (response.status().equals(HttpResponseStatus.OK)
            || response.status().equals(HttpResponseStatus.ACCEPTED)
            || response.status().equals(HttpResponseStatus.CREATED)) {
          this.client.connect(this.request);
          return;
        }
        this.promise.setFailure(new IOException("Content was not readable. HTTP Status: "
          + response.status()));
      }

      try {
        this.promise.setSuccess(request.getResponseDecoder().decode(response.headers(), response.content()));
      }
      // Catches both parsed EtcdExceptions and parsing exceptions
      catch (Exception e) {
        this.promise.setFailure(e);
      }
    }
  }
}
