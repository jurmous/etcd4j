package com.inscriptive.etcd4j.transport;

import com.inscriptive.etcd4j.requests.EtcdRequest;
import com.inscriptive.etcd4j.responses.EtcdKeysResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import com.inscriptive.etcd4j.client.exceptions.PrematureDisconnectException;

/**
 * Handles etcd responses
 *
 * @param <T>  response type
 * @param <RQ> Request type
 */
public abstract class AbstractEtcdResponseHandler<RQ extends EtcdRequest, T> extends SimpleChannelInboundHandler<T> {
  protected final Promise<EtcdKeysResponse> promise;
  protected final EtcdNettyClient client;
  protected final RQ request;

  private boolean isRetried = false;

  /**
   * Constructor
   *
   * @param etcdNettyClient the client handling connections
   * @param etcdRequest     request
   */
  @SuppressWarnings("unchecked")
  public AbstractEtcdResponseHandler(EtcdNettyClient etcdNettyClient, RQ etcdRequest) {
    this.client = etcdNettyClient;
    this.request = etcdRequest;
    this.promise = etcdRequest.getPromise().getNettyPromise();
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

  @Override public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!isRetried && !promise.isDone()) {
      this.request.getPromise().handleRetry(new PrematureDisconnectException());
    }
    super.channelUnregistered(ctx);
  }
}
