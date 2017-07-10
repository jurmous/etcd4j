package mousio.etcd4j;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdHealthResponse;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdLeaderStatsResponse;
import mousio.etcd4j.responses.EtcdMembersResponse;
import mousio.etcd4j.responses.EtcdSelfStatsResponse;
import mousio.etcd4j.responses.EtcdStoreStatsResponse;
import mousio.etcd4j.responses.EtcdVersionResponse;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Performs tests on a real server at local address. All actions are performed in "etcd4j_test" dir
 */
public class TestFunctionality {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestFunctionality.class);

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
    this.etcd = new EtcdClient();
    this.etcd.setRetryHandler(new RetryWithExponentialBackOff(20, 4, 10000));

    cleanup();
  }

  @After
  public void tearDown() throws Exception {
    cleanup();

    this.etcd.close();
    this.etcd = null;
  }

  /**
   * Test version
   *
   * @throws Exception
   */
  @Test
  public void testOldVersion() {
    String version = etcd.getVersion();
    assertNotNull(version);
    assertTrue(version.contains("etcd"));
  }

  /**
   * Test version
   *
   * @throws Exception
   */
  @Test
  public void testVersion() {
    EtcdVersionResponse version = etcd.version();
    assertNotNull(version);
    assertTrue(version.server.startsWith("2.") || version.server.startsWith("3."));
    assertTrue(version.cluster.startsWith("2.") || version.cluster.startsWith("3."));
  }


  /**
   * Test Self Stats
   *
   * @throws Exception
   */
  @Test
  public void testSelfStats() {
    EtcdSelfStatsResponse stats = etcd.getSelfStats();
    assertNotNull(stats);
    assertNotNull(stats.getLeaderInfo());
    assertEquals(stats.getId(), stats.getLeaderInfo().getLeader());
  }


  /**
   * Test leader Stats
   *
   * @throws Exception
   */
  @Test
  public void testLeaderStats() {
    EtcdLeaderStatsResponse stats = etcd.getLeaderStats();
    assertNotNull(stats);

    // stats
    assertNotNull(stats.getLeader());
    assertNotNull(stats.getFollowers());
    assertEquals(stats.getFollowers().size(), 0);
  }


  /**
   * Test Store Stats
   *
   * @throws Exception
   */
  @Test
  public void testStoreStats() {
    EtcdStoreStatsResponse stats = etcd.getStoreStats();
    assertNotNull(stats);
  }

  /**
   * Test Members
   *
   * @throws Exception
   */
  @Test
  public void testMembers() {
    EtcdMembersResponse members = etcd.getMembers();
    assertNotNull(members);
    assertTrue(members.getMembers().size() >= 1);
  }

  /**
   * Test Health
   *
   * @throws Exception
   */
  @Test
  public void testHealth() {
    EtcdHealthResponse health = etcd.getHealth();
    assertNotNull(health);
    assertTrue(health.getHealth().equals("true"));
  }

  @Test
  public void testTimeout() throws IOException, EtcdException, EtcdAuthenticationException {
    try {
      etcd.put("etcd4j_test/fooTO", "bar").timeout(1, TimeUnit.MILLISECONDS).send().get();
      fail();
    } catch (TimeoutException e) {
      // Should time out
    }
    try {
      etcd.deleteDir("etcd4j_test/fooTO").recursive().send().get();
    } catch (Exception e) {
    }
  }

  /**
   * Simple value tests
   */
  @Test
  public void testKey() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    try {
      EtcdKeysResponse response = etcd.put("etcd4j_test/foo", "bar").send().get();
      assertEquals(EtcdKeyAction.set, response.action);

      response = etcd.put("etcd4j_test/foo2", "bar").prevExist(false).send().get();
      assertEquals(EtcdKeyAction.create, response.action);

      response = etcd.put("etcd4j_test/foo", "bar1").ttl(40).prevExist(true).send().get();
      assertEquals(EtcdKeyAction.update, response.action);
      assertNotNull(response.node.expiration);

      response = etcd.put("etcd4j_test/foo", "bar2").prevValue("bar1").send().get();
      assertEquals(EtcdKeyAction.compareAndSwap, response.action);

      response = etcd.put("etcd4j_test/foo", "bar3").prevIndex(response.node.modifiedIndex).send().get();
      assertEquals(EtcdKeyAction.compareAndSwap, response.action);

      response = etcd.get("etcd4j_test/foo").consistent().send().get();
      assertEquals("bar3", response.node.value);

      // Test slash before key
      response = etcd.get("/etcd4j_test/foo").consistent().send().get();
      assertEquals("bar3", response.node.value);

      response = etcd.delete("etcd4j_test/foo").send().get();
      assertEquals(EtcdKeyAction.delete, response.action);
    } finally {
      etcd.deleteDir("etcd4j_test").recursive().send().get();
    }
  }

  /**
   * Simple value tests
   */
  @Test
  public void testError() throws IOException, EtcdAuthenticationException, TimeoutException {
    try {
      etcd.get("etcd4j_test/barf").send().get();
    } catch (EtcdException e) {
      assertEquals(100, e.errorCode);
    }

    try {
      etcd.put("etcd4j_test/barf", "huh").prevExist(true).send().get();
    } catch (EtcdException e) {
      assertEquals(100, e.errorCode);
    }
  }

  /**
   * Refresh test
   */
  @Test
  public void testRefreshTtl() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdKeysResponse initialResponse = etcd.put("etcd4j_test/foo", "bar").ttl(60).send().get();
    assertEquals(EtcdKeyAction.set, initialResponse.action);

    final EtcdKeysResponse refreshedResponse = etcd.refresh("etcd4j_test/foo", 120).send().get();

    assertEquals(initialResponse.node.createdIndex, refreshedResponse.node.createdIndex);
    assertTrue("expected ttl to be updated", refreshedResponse.node.ttl > 60);
  }

  /**
   * Tests redirect by sending a key with too many slashes.
   */
  @Test
  public void testRedirect() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    etcd.put("etcd4j_test/redirect", "bar").send().get();

    // Test redirect with a double slash
    EtcdKeysResponse response = etcd.get("//etcd4j_test/redirect").consistent().send().get();
    assertEquals("bar", response.node.value);
  }

  /**
   * Directory tests
   */
  @Test
  public void testDir() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdKeysResponse r = etcd.putDir("etcd4j_test/foo_dir").send().get();
    assertEquals(r.action, EtcdKeyAction.set);

    r = etcd.getDir("etcd4j_test/foo_dir").consistent().send().get();
    assertEquals(r.action, EtcdKeyAction.get);

    // Test slash before key
    r = etcd.getDir("/etcd4j_test/foo_dir").send().get();
    assertEquals(r.action, EtcdKeyAction.get);

    r = etcd.put("etcd4j_test/foo_dir/foo", "bar").send().get();
    assertEquals(r.node.value, "bar");

    r = etcd.putDir("etcd4j_test/foo_dir/foo_subdir").ttl(20).send().get();
    assertEquals(r.action, EtcdKeyAction.set);
    assertNotNull(r.node.expiration);

    r = etcd.deleteDir("etcd4j_test/foo_dir").recursive().send().get();
    assertEquals(r.action, EtcdKeyAction.delete);
  }

  /**
   * Recursive
   */
  @Test
  public void testRecursive() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    etcd.put("etcd4j_test/nested/root/key-1", "key1").send().get();
    etcd.put("etcd4j_test/nested/root/node-1/key-2", "key2").send().get();
    etcd.put("etcd4j_test/nested/root/node-1/child/key-3", "key3").send().get();
    etcd.put("etcd4j_test/nested/root/node-2/key-4", "key4").send().get();

    EtcdKeysResponse r;

    r =  etcd.get("etcd4j_test/nested").recursive().timeout(10, TimeUnit.SECONDS).send().get();
    assertEquals(1, r.node.nodes.size());
    assertEquals(3, r.node.nodes.get(0).nodes.size());

    r =  etcd.getDir("etcd4j_test/nested").recursive().timeout(10, TimeUnit.SECONDS).send().get();
    assertEquals(1, r.node.nodes.size());
    assertEquals(3, r.node.nodes.get(0).nodes.size());
    
    r = etcd.deleteDir("etcd4j_test/nested").recursive().send().get();
    assertEquals(r.action, EtcdKeyAction.delete);
  }

  /**
   * In order key tests
   */
  @Test
  public void testInOrderKeys() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdKeysResponse r = etcd.post("etcd4j_test/queue", "Job1").send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.post("etcd4j_test/queue", "Job2").ttl(20).send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.get(r.node.key).consistent().send().get();
    assertTrue(r.node.key.endsWith(r.node.createdIndex+""));
    assertEquals(r.node.value, "Job2");

    r = etcd.get("etcd4j_test/queue").consistent().recursive().sorted().send().get();
    assertEquals(2, r.node.nodes.size());
    assertEquals("Job2", r.node.nodes.get(1).value);

    r = etcd.deleteDir("etcd4j_test/queue").recursive().send().get();
    assertEquals(r.action, EtcdKeyAction.delete);
  }

  /**
   * In order key tests
   */
  @Test
  public void testWait() throws IOException, EtcdException, EtcdAuthenticationException, InterruptedException, TimeoutException {
    EtcdResponsePromise<EtcdKeysResponse> p = etcd.get("etcd4j_test/test").waitForChange().send();

    // Ensure the change is received after the listen command is received.
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          etcd.put("etcd4j_test/test", "changed").send().get();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
          fail();
        }
      }
    }, 20);

    EtcdKeysResponse r = p.get();
    assertEquals("changed", r.node.value);
  }

  @Test(expected = TimeoutException.class)
  public void testWaitTimeout() throws IOException, EtcdException, EtcdAuthenticationException, InterruptedException, TimeoutException {
    etcd.get("etcd4j_test/test").waitForChange().timeout(1, TimeUnit.SECONDS).send().get();

    // get should have thrown TimeoutException
    fail();
  }

  @Test(timeout = 1000)
  public void testChunkedData() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    //creating very long key to force content to be chunked
    StringBuilder stringBuilder = new StringBuilder(15000);
    for (int i = 0; i < 15000; i++) {
      stringBuilder.append("a");
    }
    EtcdKeysResponse response = etcd.put("etcd4j_test/foo", stringBuilder.toString()).send().get();
    assertEquals(EtcdKeyAction.set, response.action);
  }

  @Test
  public void testIfCleanClose() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdClient client = new EtcdClient();
    client.setRetryHandler(new RetryWithExponentialBackOff(20, 4, 1000));

    EtcdResponsePromise<EtcdKeysResponse> p = client.get("etcd4j_test/test").waitForChange().send();
    client.close();

    try {
      p.get();
      fail();
    } catch (IOException e){
      // should be catched because connection was canceled
      if (!(e.getCause() instanceof CancellationException)) {
        fail();
      }
    }
  }

  @Test
  public void testGetAll() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdKeysResponse.EtcdNode root;
    List<EtcdKeysResponse.EtcdNode> nodes;

    EtcdClient client = new EtcdClient();

    root = client.getAll().timeout(30, TimeUnit.SECONDS).send().get().getNode();
    nodes = root.getNodes();

    LOGGER.info("Nodes (1) {}", nodes);

    assertNotNull(nodes);
    assertTrue(root.isDir());

    client.put("etcd4j_testGetAll_1/foo1", "bar").prevExist(false).send().get();
    client.put("etcd4j_testGetAll_2/foo1", "bar").prevExist(false).send().get();

    root = client.getAll().timeout(30, TimeUnit.SECONDS).send().get().getNode();
    nodes = root.getNodes();

    LOGGER.info("Nodes (2) {}", nodes);

    assertNotNull(nodes);
    assertEquals(2, nodes.size());
  }

  @Test
  public void testGetHugeDir() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdNettyConfig config = new EtcdNettyConfig();
    config.setMaxFrameSize(1024 * 1024); // Desired max size

    EtcdNettyClient nettyClient = new EtcdNettyClient(config, URI.create("http://localhost:4001"));

    EtcdClient client = new EtcdClient(nettyClient);

    for (int i = 0; i < 2000; i++) {
      client.put("/etcd4j_test/huge-dir/node-" + i, "bar").send().get();
    }

    List<EtcdKeysResponse.EtcdNode> nodes;

    nodes = client.getDir("/etcd4j_test/huge-dir/").send().get().getNode().getNodes();
    assertNotNull(nodes);
    assertEquals(2000, nodes.size());
  }
}
