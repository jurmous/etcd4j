package mousio.etcd4j.transport;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsServerAddresses;
import io.netty.util.concurrent.Promise;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NettyTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(NettyTest.class);

  @Ignore
  @Test
  public void test() throws Exception {
    NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    Bootstrap bootstrap = new Bootstrap()
      .group(eventLoopGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300)
      .resolver(new DnsAddressResolverGroup(
        NioDatagramChannel.class,
        DnsServerAddresses.defaultAddresses()))
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          p.addLast("codec", new HttpClientCodec());
          p.addLast("chunkedWriter", new ChunkedWriteHandler());
          p.addLast("aggregate", new HttpObjectAggregator(1024 * 100));
        }
      });

    final ChannelFuture cf = bootstrap.connect(InetSocketAddress.createUnresolved("localhost",4001));
    final Promise<Object> p = cf.channel().eventLoop().newPromise();

    cf.addListener((ChannelFuture f) -> {
      if (f.isSuccess()) {
        p.setSuccess("Success");
      } else {
        p.setFailure(f.cause());
      }

      f.channel().close();
    });

    eventLoopGroup.shutdownGracefully();
    p.await();
  }
}
