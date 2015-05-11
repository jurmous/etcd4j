package com.inscriptive.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class EtcdNettyClientTest {

  @Test
  public void testConfig() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();

    URI uri = URI.create("http://localhost:4001");

    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");

    EtcdNettyClient client = new EtcdNettyClient(config, null, uri);

    Bootstrap bootstrap = client.getBootstrap();

    assertEquals(evl, bootstrap.group());

    Channel channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();

    assertEquals(100, channel.config().getOption(ChannelOption.CONNECT_TIMEOUT_MILLIS).intValue());
  }
}