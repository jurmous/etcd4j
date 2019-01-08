package mousio.etcd4j.transport;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import mousio.client.retry.RetryNTimes;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.support.EtcdCluster;
import mousio.etcd4j.support.EtcdClusterFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EtcdNettyClientTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(EtcdNettyClientTest.class);

  private static EtcdCluster CLUSTER;
  
  @BeforeClass
  public static void setUpCluster() {
    CLUSTER = EtcdClusterFactory.buildCluster(EtcdNettyClientTest.class.getName(), 1, false);
    CLUSTER.start();
  }

  @AfterClass
  public static void tearDownCluster() {
    CLUSTER.close();
  }

  @Test
  public void testConfig() throws Exception {
    try {
      NioEventLoopGroup evl = new NioEventLoopGroup();
      EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");

      URI[] endpoints = CLUSTER.endpoints();
      EtcdNettyClient client = new EtcdNettyClient(config, endpoints);
      Bootstrap bootstrap = client.getBootstrap();
      Channel channel = bootstrap.connect(endpoints[0].getHost(), endpoints[0].getPort()).sync().channel();

      assertEquals(evl, bootstrap.config().group());
      assertEquals(100, channel.config().getOption(ChannelOption.CONNECT_TIMEOUT_MILLIS).intValue());
    } catch (Throwable t) {
      LOGGER.warn("", t);
      throw t;
    }
  }

  @Test
  public void testManagedEventLoopGroup() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();
    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl, false)
        .setHostName("localhost");

    URI[] endpoints = CLUSTER.endpoints();
    EtcdNettyClient client = new EtcdNettyClient(config, endpoints);
    client.close();

    assertTrue(!(evl.isShuttingDown() || evl.isShutdown() || evl.isTerminated()));
    
    evl.shutdownGracefully();
    assertTrue(evl.isShuttingDown() || evl.isShutdown() || evl.isTerminated());
  }

  @Test
  public void testCustomEtcdNettyClient() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();
    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");


    URI[] endpoints = CLUSTER.endpoints();
    EtcdNettyClient client = new EtcdNettyClient(config, endpoints);
    EtcdClient etcdClient = new EtcdClient(client);
    etcdClient.setRetryHandler(new RetryNTimes(0, 0));

    assertNotNull(etcdClient.version());
  }

  @Test
  public void testEtcdClientClose() throws Exception {
    NioEventLoopGroup evl = new NioEventLoopGroup();
    EtcdNettyConfig config = new EtcdNettyConfig()
        .setConnectTimeout(100)
        .setSocketChannelClass(NioSocketChannel.class)
        .setMaxFrameSize(1024 * 1024)
        .setEventLoopGroup(evl)
        .setHostName("localhost");

    assertTrue(config.isManagedEventLoopGroup());

    URI[] endpoints = CLUSTER.endpoints();
    EtcdNettyClient client = new EtcdNettyClient(config, endpoints);
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
    URI[] endpoints = CLUSTER.endpoints();
    EtcdClient client = new EtcdClient("test", "test", endpoints);
    assertNotNull(client.get("/test/messages").send().get());
  }

  @Ignore
  @Test(expected = EtcdAuthenticationException.class)
  public void testAuthFailure() throws Exception {
    URI[] endpoints = CLUSTER.endpoints();
    EtcdClient client = new EtcdClient("test", "test_", endpoints);
    client.get("/test/messages").send().get();
  }
}
