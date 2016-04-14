package mousio.etcd4j.responses;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;

import static mousio.etcd4j.EtcdUtil.convertDate;
import static mousio.etcd4j.responses.EtcdResponseDecoders.*;
import static org.junit.Assert.*;

/**
 * Examples are taken out of the api.md of etcd project.
 */
public class EtcdKeysResponseParserTest {

  private HttpHeaders headers;

  @Before
  public void setup() {
    this.headers = new DefaultHttpHeaders();
    this.headers.add(X_ETCD_CLUSTER_ID, "test");
    this.headers.add(X_ETCD_INDEX, 208);
    this.headers.add(X_RAFT_INDEX, 5);
    this.headers.add(X_RAFT_TERM, 15);
  }

  @Test
  public void testParseSetKey() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"set\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 2,\n" +
        "        \"value\": \"Hello world\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.set, action.action);
    assertEquals(2, action.node.createdIndex.intValue());
    assertEquals("/message", action.node.key);
    assertEquals(2, action.node.modifiedIndex.intValue());
    assertEquals("Hello world", action.node.value);

    assertEquals("test", action.etcdClusterId);
    assertEquals(208, action.etcdIndex.longValue());
    assertEquals(5, action.raftIndex.longValue());
    assertEquals(15, action.raftTerm.longValue());
  }

  @Test
  public void testParseGetKey() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"get\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 2,\n" +
        "        \"value\": \"Hello world\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.get, action.action);
    assertEquals(2, action.node.createdIndex.intValue());
    assertEquals("/message", action.node.key);
    assertEquals(2, action.node.modifiedIndex.intValue());
    assertEquals("Hello world", action.node.value);
  }

  @Test
  public void testParseChangeKey() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"set\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 3,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 3,\n" +
        "        \"value\": \"Hello etcd\"\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"value\": \"Hello world\",\n" +
        "        \"modifiedIndex\": 2\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.set, action.action);
    assertEquals(3, action.node.createdIndex.intValue());
    assertEquals("/message", action.node.key);
    assertEquals(3, action.node.modifiedIndex.intValue());
    assertEquals("Hello etcd", action.node.value);

    assertEquals(2, action.prevNode.createdIndex.intValue());
    assertEquals("/message", action.prevNode.key);
    assertEquals(2, action.prevNode.modifiedIndex.intValue());
    assertEquals("Hello world", action.prevNode.value);
  }

  @Test
  public void testParseDeleteKey() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"delete\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 3,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 4\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"key\": \"/message\",\n" +
        "        \"value\": \"Hello etcd\",\n" +
        "        \"modifiedIndex\": 3,\n" +
        "        \"createdIndex\": 3\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.delete, action.action);
    assertEquals(3, action.node.createdIndex.intValue());
    assertEquals("/message", action.node.key);
    assertEquals(4, action.node.modifiedIndex.intValue());

    assertEquals(3, action.prevNode.createdIndex.intValue());
    assertEquals("/message", action.prevNode.key);
    assertEquals(3, action.prevNode.modifiedIndex.intValue());
    assertEquals("Hello etcd", action.prevNode.value);
  }

  @Test
  public void testParseSetKeyTtl() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"set\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 5,\n" +
        "        \"expiration\": \"2013-12-04T12:01:21.874888581-08:00\",\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 5,\n" +
        "        \"ttl\": 5,\n" +
        "        \"value\": \"bar\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.set, action.action);
    assertEquals(5, action.node.createdIndex.intValue());
    assertEquals("/foo", action.node.key);
    assertEquals(5, action.node.modifiedIndex.intValue());
    assertEquals("bar", action.node.value);
    assertEquals(5, action.node.ttl.intValue());
    assertEquals(convertDate("2013-12-04T12:01:21.874888581-08:00"), action.node.expiration);
  }

  @Test
  public void testParseTtlExpiredException() throws Exception {
      EtcdException e = EtcdException.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
          "    \"cause\": \"/foo\",\n" +
          "    \"errorCode\": 100,\n" +
          "    \"index\": 6,\n" +
          "    \"message\": \"Key Not Found\"\n" +
          "}").getBytes()));

      assertEquals(100, e.errorCode);
      assertEquals("/foo", e.etcdCause);
      assertEquals(6, e.index.intValue());
      assertEquals("Key Not Found", e.etcdMessage);
  }

  @Test
  public void testParseUpdateKeyTtl() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"update\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 5,\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 6,\n" +
        "        \"value\": \"bar\"\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"createdIndex\": 5,\n" +
        "        \"expiration\": \"2013-12-04T12:01:21.874888581-08:00\",\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 5,\n" +
        "        \"ttl\": 3,\n" +
        "        \"value\": \"bar\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.update, action.action);
    assertEquals(5, action.node.createdIndex.intValue());
    assertEquals("/foo", action.node.key);
    assertEquals(6, action.node.modifiedIndex.intValue());
    assertEquals("bar", action.node.value);

    assertEquals(5, action.prevNode.createdIndex.intValue());
    assertEquals("/foo", action.prevNode.key);
    assertEquals(5, action.prevNode.modifiedIndex.intValue());
    assertEquals("bar", action.prevNode.value);
    assertEquals(3, action.prevNode.ttl.intValue());
    assertEquals(convertDate("2013-12-04T12:01:21.874888581-08:00"), action.prevNode.expiration);
  }

  @Test
  public void testParseCreateKey() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"create\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 6,\n" +
        "        \"key\": \"/queue/6\",\n" +
        "        \"modifiedIndex\": 6,\n" +
        "        \"value\": \"Job1\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.create, action.action);
    assertEquals(6, action.node.createdIndex.intValue());
    assertEquals("/queue/6", action.node.key);
    assertEquals(6, action.node.modifiedIndex.intValue());
    assertEquals("Job1", action.node.value);
  }

  @Test
  public void testParseGetOrderedKeys() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"get\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"dir\": true,\n" +
        "        \"key\": \"/queue\",\n" +
        "        \"modifiedIndex\": 2,\n" +
        "        \"nodes\": [\n" +
        "            {\n" +
        "                \"createdIndex\": 2,\n" +
        "                \"key\": \"/queue/2\",\n" +
        "                \"modifiedIndex\": 2,\n" +
        "                \"value\": \"Job1\"\n" +
        "            },\n" +
        "            {\n" +
        "                \"createdIndex\": 3,\n" +
        "                \"key\": \"/queue/3\",\n" +
        "                \"modifiedIndex\": 3,\n" +
        "                \"value\": \"Job2\"\n" +
        "            }\n" +
        "        ]\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.get, action.action);
    assertEquals(2, action.node.createdIndex.intValue());
    assertEquals("/queue", action.node.key);
    assertEquals(2, action.node.modifiedIndex.intValue());
    assertTrue(action.node.dir);

    assertEquals(2, action.node.nodes.size());

    assertEquals(2, action.node.nodes.get(0).createdIndex.intValue());
    assertEquals(2, action.node.nodes.get(0).modifiedIndex.intValue());
    assertEquals("Job1", action.node.nodes.get(0).value);
    assertEquals("/queue/2", action.node.nodes.get(0).key);

    assertEquals(3, action.node.nodes.get(1).createdIndex.intValue());
    assertEquals(3, action.node.nodes.get(1).modifiedIndex.intValue());
    assertEquals("Job2", action.node.nodes.get(1).value);
    assertEquals("/queue/3", action.node.nodes.get(1).key);
  }


  @Test
  public void testParseExpiredDir() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"expire\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 8,\n" +
        "        \"key\": \"/dir\",\n" +
        "        \"modifiedIndex\": 15\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"createdIndex\": 8,\n" +
        "        \"key\": \"/dir\",\n" +
        "        \"dir\":true,\n" +
        "        \"modifiedIndex\": 17,\n" +
        "        \"expiration\": \"2013-12-11T10:39:35.689275857-08:00\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.expire, action.action);
    assertEquals(8, action.node.createdIndex.intValue());
    assertEquals("/dir", action.node.key);
    assertEquals(15, action.node.modifiedIndex.intValue());

    assertEquals(8, action.prevNode.createdIndex.intValue());
    assertEquals("/dir", action.prevNode.key);
    assertEquals(17, action.prevNode.modifiedIndex.intValue());

    assertEquals(convertDate("2013-12-11T10:39:35.689275857-08:00"), action.prevNode.expiration);

    assertTrue(action.prevNode.dir);
  }

  @Test
  public void testParseCompareAndSwap() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"compareAndSwap\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 8,\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 9,\n" +
        "        \"value\": \"two\"\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"createdIndex\": 8,\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 8,\n" +
        "        \"value\": \"one\"\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.compareAndSwap, action.action);
    assertEquals(8, action.node.createdIndex.intValue());
    assertEquals("/foo", action.node.key);
    assertEquals(9, action.node.modifiedIndex.intValue());
    assertEquals("two", action.node.value);

    assertEquals(8, action.prevNode.createdIndex.intValue());
    assertEquals("/foo", action.prevNode.key);
    assertEquals(8, action.prevNode.modifiedIndex.intValue());
    assertEquals("one", action.prevNode.value);
  }

  @Test
  public void testParseCompareAndDelete() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"compareAndDelete\",\n" +
        "    \"node\": {\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 9,\n" +
        "        \"createdIndex\": 8\n" +
        "    },\n" +
        "    \"prevNode\": {\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"value\": \"one\",\n" +
        "        \"modifiedIndex\": 8,\n" +
        "        \"createdIndex\": 8\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.compareAndDelete, action.action);
    assertEquals(8, action.node.createdIndex.intValue());
    assertEquals("/foo", action.node.key);
    assertEquals(9, action.node.modifiedIndex.intValue());

    assertEquals(8, action.prevNode.createdIndex.intValue());
    assertEquals("/foo", action.prevNode.key);
    assertEquals(8, action.prevNode.modifiedIndex.intValue());
    assertEquals("one", action.prevNode.value);
  }

  @Test
  public void testParseRecursiveGet() throws Exception {
    EtcdKeysResponse action = EtcdKeysResponse.DECODER.decode(headers, Unpooled.copiedBuffer(("{\n" +
        "    \"action\": \"get\",\n" +
        "    \"node\": {\n" +
        "        \"dir\": true,\n" +
        "        \"key\": \"/\",\n" +
        "        \"nodes\": [\n" +
        "            {\n" +
        "                \"createdIndex\": 2,\n" +
        "                \"dir\": true,\n" +
        "                \"key\": \"/foo_dir\",\n" +
        "                \"modifiedIndex\": 2,\n" +
        "                \"nodes\": [\n" +
        "                    {\n" +
        "                        \"createdIndex\": 2,\n" +
        "                        \"key\": \"/foo_dir/foo\",\n" +
        "                        \"modifiedIndex\": 2,\n" +
        "                        \"value\": \"bar\"\n" +
        "                    }\n" +
        "                ]\n" +
        "            }\n" +
        "        ]\n" +
        "    }\n" +
        "}").getBytes()));

    assertEquals(EtcdKeyAction.get, action.action);
    assertTrue(action.node.dir);

    assertEquals(1, action.node.nodes.size());

    assertEquals(2, action.node.nodes.get(0).createdIndex.intValue());
    assertEquals("/foo_dir", action.node.nodes.get(0).key);
    assertEquals(2, action.node.nodes.get(0).modifiedIndex.intValue());
    assertTrue(action.node.nodes.get(0).dir);

    assertEquals(2, action.node.nodes.get(0).nodes.get(0).createdIndex.intValue());
    assertEquals("/foo_dir/foo", action.node.nodes.get(0).nodes.get(0).key);
    assertEquals(2, action.node.nodes.get(0).nodes.get(0).modifiedIndex.intValue());
    assertEquals("bar", action.node.nodes.get(0).nodes.get(0).value);
  }

  @Test
  public void testErrorCode() throws Exception {
    EtcdException e = EtcdException.DECODER.decode(headers, Unpooled.copiedBuffer((
        "{\n" +
        "   \"errorCode\": 105,\n" +
        "   \"message\": \"Key already exists\",\n" +
        "   \"cause\": \"/foo/bar\",\n" +
        "   \"index\": 1024\n" +
        "}").getBytes()));

    assertTrue(e.isErrorCode(EtcdErrorCode.NodeExist));
    assertNotEquals(e.getErrorCode(), EtcdErrorCode.KeyNotFound);
  }
}
