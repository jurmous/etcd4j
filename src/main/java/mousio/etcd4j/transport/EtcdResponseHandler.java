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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Promise;
import mousio.client.exceptions.PrematureDisconnectException;
import mousio.etcd4j.requests.EtcdRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  private static final Map<HttpResponseStatus, EtcdResponseDecoder<? extends Throwable>> failureDecoders;

  static {
    failureDecoders = new HashMap<>();
    failureDecoders.put(HttpResponseStatus.UNAUTHORIZED, EtcdAuthenticationException.DECODER);
    failureDecoders.put(HttpResponseStatus.NOT_FOUND, EtcdException.DECODER);
    failureDecoders.put(HttpResponseStatus.FORBIDDEN, EtcdException.DECODER);
    failureDecoders.put(HttpResponseStatus.PRECONDITION_FAILED, EtcdException.DECODER);
    failureDecoders.put(HttpResponseStatus.INTERNAL_SERVER_ERROR, EtcdException.DECODER);
  }

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
   * If true the promise will not fail on un-registering this handler.
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
    final HttpResponseStatus status =response.status();
    final HttpHeaders headers = response.headers();
    final ByteBuf content = response.content();

    if (logger.isDebugEnabled()) {
      logger.debug("Received {} for {} {}",
        status.code(), this.request.getMethod().name(), this.request.getUri());
    }

    if (status.equals(HttpResponseStatus.MOVED_PERMANENTLY)
      || status.equals(HttpResponseStatus.TEMPORARY_REDIRECT)) {
      if (headers.contains(HttpHeaderNames.LOCATION)) {
        this.request.setUrl(headers.get(HttpHeaderNames.LOCATION));
        this.client.connect(this.request);
        // Closing the connection which handled the previous request.
        ctx.close();
        if (logger.isDebugEnabled()) {
          logger.debug("redirect for {} to {}",
            this.request.getHttpRequest().uri() ,
            headers.get(HttpHeaderNames.LOCATION));
        }
      } else {
        this.promise.setFailure(new Exception("Missing Location header on redirect"));
      }
    } else {
      EtcdResponseDecoder<? extends Throwable> failureDecoder = failureDecoders.get(status);
      if(failureDecoder != null) {
        this.promise.setFailure(failureDecoder.decode(headers, content));
      } else if (!content.isReadable()) {
        // If connection was accepted maybe response has to be waited for
        if (status.equals(HttpResponseStatus.OK)
          || status.equals(HttpResponseStatus.ACCEPTED)
          || status.equals(HttpResponseStatus.CREATED)) {
          this.client.connect(this.request);
        } else {
          this.promise.setFailure(new IOException(
            "Content was not readable. HTTP Status: " + status));
        }
      } else {
        try {
          this.promise.setSuccess(
            request.getResponseDecoder().decode(headers, content));
        } catch (Exception e) {
          // Catches both parsed EtcdExceptions and parsing exceptions
          this.promise.setFailure(e);
        }
      }
    }
  }
}
