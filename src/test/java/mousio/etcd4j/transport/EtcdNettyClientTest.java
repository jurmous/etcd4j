package mousio.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import mousio.client.retry.RetryNTimes;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    EtcdNettyClient client = new EtcdNettyClient(config, uri);
    Bootstrap bootstrap = client.getBootstrap();

    assertEquals(evl, bootstrap.group());

    Channel channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();

    assertEquals(100, channel.config().getOption(ChannelOption.CONNECT_TIMEOUT_MILLIS).intValue());
  }

  @Test
  public void testManagedEventLoopGroup() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();

    URI uri = URI.create("http://localhost:4001");

    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl, false)
        .setHostName("localhost");

    EtcdNettyClient client = new EtcdNettyClient(config, uri);
    client.close();

    assertTrue(!(evl.isShuttingDown() || evl.isShutdown() || evl.isTerminated()));
    
    evl.shutdownGracefully();
    assertTrue(evl.isShuttingDown() || evl.isShutdown() || evl.isTerminated());
  }

  @Ignore
  @Test
  public void testCustomEtcdNettyClient() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();

    URI uri = URI.create("http://localhost:4001");

    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");

    EtcdNettyClient client = new EtcdNettyClient(config, uri);
    EtcdClient etcdClient = new EtcdClient(client);
    etcdClient.setRetryHandler(new RetryNTimes(0, 0));

    assertNotNull(etcdClient.version());
  }

  @Ignore
  @Test
  public void testEtcdClientClose() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();

    URI uri = URI.create("http://localhost:4001");

    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");

    assertTrue(config.isManagedEventLoopGroup());
    assertTrue(config.isManagedTimer());

    EtcdNettyClient client = new EtcdNettyClient(config, uri);
    EtcdClient etcdClient = new EtcdClient(client);
    etcdClient.setRetryHandler(new RetryNTimes(500, 2));

    try {
      etcdClient.put("foo", "bar").send().get();
    } catch (Throwable e) {
    }

    try {
      etcdClient.close();
    } catch (Throwable e) {
    }

    assertTrue(evl.isShuttingDown() || evl.isShutdown() || evl.isTerminated());
  }

  @Ignore
  @Test
  public void testAuth() throws Exception {
    EtcdClient client = new EtcdClient(
      "test",
      "test",
      URI.create("http://localhost:4001"));

    assertNotNull(client.get("/test/messages").send().get());
  }

  @Ignore
  @Test(expected = EtcdAuthenticationException.class)
  public void testAuthFailure() throws Exception {
    EtcdClient client = new EtcdClient(
      "test",
      "test_",
      URI.create("http://localhost:4001"));

    client.get("/test/messages").send().get();
  }
}
