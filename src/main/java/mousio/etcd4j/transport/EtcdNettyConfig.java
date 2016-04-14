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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jurriaan Mous
 *
 * Settings for the etcd Netty client
 */
public class EtcdNettyConfig implements Cloneable {
  private static final Logger logger = LoggerFactory.getLogger(EtcdNettyConfig.class);

  private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

  private boolean managedEventLoopGroup = true;

  private HashedWheelTimer timer = new HashedWheelTimer();

  private boolean managedTimer = true;

  private Class<? extends SocketChannel> socketChannelClass = NioSocketChannel.class;

  private int connectTimeout = 300;

  private int maxFrameSize = 1024 * 100;

  private String hostName;

  /**
   * Constructor
   */
  public EtcdNettyConfig() {
    String frameSize = System.getProperty("mousio.etcd4j.maxFrameSize");
    if (frameSize != null) {
      logger.warn("Setting mousio.etcd4j.maxFrameSize through system propery is deprecated. " +
          "Please use the EtcdNettyConfig class");
      maxFrameSize = Integer.parseInt(frameSize);
    }
  }

  /**
   * Get the connect timeout
   *
   * @return the connect timeout
   */
  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Set the connect timeout
   *
   * @param connectTimeout to set
   * @return itself for chaining.
   */
  public EtcdNettyConfig setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  /**
   * Set a custom event loop group. For use within existing netty architectures
   *
   * Note:
   *
   * When etcd client close, the event loop group will also be closed, if you don't
   * want this behavior, use {@link EtcdNettyConfig#setEventLoopGroup(EventLoopGroup, boolean)}
   *
   * @param eventLoopGroup to set.
   * @return itself for chaining.
   */
  public EtcdNettyConfig setEventLoopGroup(EventLoopGroup eventLoopGroup) {
    return setEventLoopGroup(eventLoopGroup, true);
  }

  /**
   * Set a custom event loop group. For use within existing netty architectures
   *
   * @param eventLoopGroup eventLoopGroup to set.
   * @param managed whether event loop group will be closed when etcd client close, true represent yes
   * @return itself for chaining.
   */
  public EtcdNettyConfig setEventLoopGroup(EventLoopGroup eventLoopGroup, boolean managed) {
    if (this.managedEventLoopGroup) { // if i manage it, close the old when new one come
      this.eventLoopGroup.shutdownGracefully();
    }
    this.eventLoopGroup = eventLoopGroup;
    this.managedEventLoopGroup = managed;
    return this;
  }

  /**
   * Get the current event loop group. If it was never set it will use one loop group
   * for al etcd clients
   *
   * @return Event loop group.
   */
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  /**
   * Get whether event loop group will be closed when etcd client close, true represent yes
   *
   * @return whether etcd client manage event loop group
   */
  public boolean isManagedEventLoopGroup() {
    return managedEventLoopGroup;
  }

  /**
   * Set a custom timer use to retry failed request
   *
   * @param timer custom timer
   * @return itself for chaining.
   */
  public EtcdNettyConfig setTimer(HashedWheelTimer timer) {
    setTimer(timer, true);
    return this;
  }

  /**
   * Set a custom timer use to retry failed request
   *
   * @param timer custom timer
   * @param managed whether timer will be closed when etcd client close, true represent yes
   * @return itself for chaining.
   */
  public EtcdNettyConfig setTimer(HashedWheelTimer timer, boolean managed) {
    if (this.managedTimer) { // if i manage it, close the old when new one come
      this.timer.stop();
    }
    this.timer = timer;
    this.managedTimer = managed;
    return this;
  }

  /**
   * Get timer
   *
   * @return HashedWheelTimer.
   */
  public HashedWheelTimer getTimer() {
    return timer;
  }

  /**
   * Get managedTimer
   *
   * @return managedTimer
   */
  public boolean isManagedTimer() {
    return managedTimer;
  }

  /**
   * Get the max frame size
   *
   * @return max frame size
   */
  public int getMaxFrameSize() {
    return maxFrameSize;
  }

  /**
   * Set the max frame size
   *
   * @param maxFrameSize to set
   * @return itself for chaining.
   */
  public EtcdNettyConfig setMaxFrameSize(int maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
    return this;
  }

  /**
   * Get Socket channel class
   *
   * @return Socket channel class. Default is NioSocketChannel.class
   */
  public Class<? extends SocketChannel> getSocketChannelClass() {
    return socketChannelClass;
  }

  /**
   * Set Socket channel class. Default is NioSocketChannel.class
   *
   * @param socketChannelClass to set
   * @return itself for chaining
   */
  public EtcdNettyConfig setSocketChannelClass(Class<? extends SocketChannel> socketChannelClass) {
    this.socketChannelClass = socketChannelClass;
    return this;
  }

  public boolean hasHostName() {
    return hostName != null && !hostName.trim().isEmpty();
  }

  /**
   * Get the local host name
   *
   * @return local host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Set the host name for the local machine.
   *
   * @param hostName name of local host
   * @return itself for chaining
   */
  public EtcdNettyConfig setHostName(String hostName) {
    this.hostName = hostName;
    return this;
  }

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  @Override
  public EtcdNettyConfig clone() {
    try {
      return (EtcdNettyConfig) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }
}
