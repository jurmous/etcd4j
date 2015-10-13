package mousio.etcd4j;

import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Performs tests on a real server at local address. All actions are performed in "etcd4j_test" dir
 */
public class TestFunctionality {

  private EtcdClient etcd;

  @Before
  public void setUp() throws Exception {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    this.etcd = new EtcdClient();
    this.etcd.setRetryHandler(new RetryWithExponentialBackOff(20, 4, -1));
  }

  /**
   * Test version
   *
   * @throws Exception
   */
  @Test
  public void testOldVersion() {
    assertTrue(etcd.getVersion().contains("etcd"));
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
    assertTrue(version.server.startsWith("2."));
    assertTrue(version.cluster.startsWith("2."));
  }

  @Test
  public void testTimeout() throws IOException, EtcdException, EtcdAuthenticationException {
    try {
      etcd.put("etcd4j_test/fooTO", "bar").timeout(1, TimeUnit.MILLISECONDS).send().get();
      fail();
    } catch (TimeoutException e) {
      // Should time out
    }
  }

  /**
   * Simple value tests
   */
  @Test
  public void testKey() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
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
   * In order key tests
   */
  @Test
  public void testInOrderKeys() throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException {
    EtcdKeysResponse r = etcd.post("etcd4j_test/queue", "Job1").send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.post("etcd4j_test/queue", "Job2").ttl(20).send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.get(String.format("etcd4j_test/queue/%020d",r.node.createdIndex)).consistent().send().get();
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
    EtcdResponsePromise<EtcdKeysResponse> p = etcd.get("etcd4j_test/test").waitForChange().timeout(10, TimeUnit.MILLISECONDS).send();

    EtcdKeysResponse r = p.get();
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

  @After
  public void tearDown() throws Exception {
    try {
      etcd.deleteDir("etcd4j_test").recursive().send().get();
    } catch (EtcdException | IOException e) {
      // ignore since not all tests create the directory
    }
    this.etcd.close();
  }
}
