package mousio.etcd4j;

import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * Performs tests on a real server at local address.
 * All actions are performed in "etcd4j_test" dir
 */
public class TestFunctionality {

  private EtcdClient etcd;

  @Before
  public void setUp() throws Exception {
    this.etcd = new EtcdClient();
  }

  /**
   * Test version
   *
   * @throws Exception
   */
  @Test
  public void testVersion() {
    assertTrue(etcd.getVersion().startsWith("etcd "));
  }

  /**
   * Simple value tests
   */
  @Test
  public void testKey() throws IOException, EtcdException, TimeoutException {
    EtcdKeysResponse response = etcd.put("etcd4j_test/foo", "bar").send().get();
    assertEquals(EtcdKeyAction.set, response.action);

    response = etcd.put("etcd4j_test/foo", "bar1").ttl(40).prevExist().send().get();
    assertEquals(EtcdKeyAction.update, response.action);
    assertNotNull(response.node.expiration);

    response = etcd.put("etcd4j_test/foo", "bar2").prevValue("bar1").send().get();
    assertEquals(EtcdKeyAction.compareAndSwap, response.action);

    response = etcd.put("etcd4j_test/foo", "bar3").prevIndex(response.node.modifiedIndex).send().get();
    assertEquals(EtcdKeyAction.compareAndSwap, response.action);

    response = etcd.get("etcd4j_test/foo").send().get();
    assertEquals("bar3", response.node.value);

    // Test redirect
    response = etcd.get("/etcd4j_test/foo").send().get();
    assertEquals("bar3", response.node.value);

    response = etcd.delete("etcd4j_test/foo").send().get();
    assertEquals(EtcdKeyAction.delete, response.action);
  }


  /**
   * Directory tests
   */
  @Test
  public void testDir() throws IOException, EtcdException, TimeoutException {
    EtcdKeysResponse r = etcd.putDir("etcd4j_test/foo_dir").send().get();
    assertEquals(r.action, EtcdKeyAction.set);

    r = etcd.getDir("etcd4j_test/foo_dir").send().get();
    assertEquals(r.action, EtcdKeyAction.get);

    // Test redirect
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
  public void testInOrderKeys() throws IOException, EtcdException, TimeoutException {
    EtcdKeysResponse r = etcd.post("etcd4j_test/queue", "Job1").send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.post("etcd4j_test/queue", "Job2").ttl(20).send().get();
    assertEquals(r.action, EtcdKeyAction.create);

    r = etcd.get("etcd4j_test/queue/" + r.node.createdIndex).send().get();
    assertEquals(r.node.value, "Job2");

    r = etcd.get("etcd4j_test/queue").recursive().sorted().send().get();
    assertEquals(2, r.node.nodes.size());
    assertEquals("Job2", r.node.nodes.get(1).value);

    r = etcd.deleteDir("etcd4j_test/queue").recursive().send().get();
    assertEquals(r.action, EtcdKeyAction.delete);
  }

  /**
   * In order key tests
   */
  @Test
  public void testWait() throws IOException, EtcdException, InterruptedException, TimeoutException {
    EtcdResponsePromise<EtcdKeysResponse> p = etcd.get("etcd4j_test/test").waitForChange().send();

    // Ensure the change is received after the listen command is received.
    new Timer().schedule(new TimerTask() {
      @Override public void run() {
        try {
          etcd.put("etcd4j_test/test", "changed").send().get();
        } catch (IOException | EtcdException | TimeoutException e) {
          fail();
        }
      }
    }, 20);

    EtcdKeysResponse r = p.get();
    assertEquals("changed", r.node.value);
  }

  @After
  public void tearDown() throws Exception {
    try {
      etcd.deleteDir("etcd4j_test").recursive().send().get();
    } catch (EtcdException e) {
      // ignore since not all tests create the directory
    }
    this.etcd.close();
  }
}