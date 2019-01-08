package mousio.etcd4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.support.EtcdCluster;
import mousio.etcd4j.support.EtcdClusterFactory;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestHugeDir {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestHugeDir.class);

  private EtcdCluster cluster;
  private EtcdClient etcd;

  protected void cleanup() {
    try {
      for (EtcdKeysResponse.EtcdNode node: etcd.getAll().send().get().getNode().getNodes()) {
        if (node.isDir()) {
          LOGGER.info("Delete dir {}", node.key);
          etcd.deleteDir(node.key).recursive().send().get();
        } else {
          LOGGER.info("Delete entry {}", node.key);
          etcd.delete(node.key).send().get();
        }
      }
    } catch (Exception e) {
    }
  }

  @Before
  public void setUp() throws Exception {
    this.cluster = EtcdClusterFactory.buildCluster(getClass().getName(), 1, false);
    this.cluster.start();

    EtcdNettyConfig config = new EtcdNettyConfig();
    config.setMaxFrameSize(1024 * 1024); // Desired max size

    EtcdNettyClient nettyClient = new EtcdNettyClient(config, cluster.endpoints());

    this.etcd = new EtcdClient(nettyClient);
    this.etcd.setRetryHandler(new RetryWithExponentialBackOff(20, 4, 10000));

    cleanup();
  }

  @After
  public void tearDown() throws Exception {
    cleanup();

    this.etcd.close();
    this.etcd = null;

    this.cluster.close();
    this.cluster = null;
  }


  @Test
  public void testGetHugeDir() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    for (int i = 0; i < 2000; i++) {
      etcd.put("/etcd4j_test/huge-dir/node-" + i, "bar").send().get();
    }

    List<EtcdKeysResponse.EtcdNode> nodes;

    nodes = etcd.getDir("/etcd4j_test/huge-dir/").send().get().getNode().getNodes();
    assertNotNull(nodes);
    assertEquals(2000, nodes.size());
  }
}
