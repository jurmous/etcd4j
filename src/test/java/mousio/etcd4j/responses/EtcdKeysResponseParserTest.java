package mousio.etcd4j.responses;

import io.netty.buffer.Unpooled;
import mousio.etcd4j.EtcdUtil;
import org.junit.Test;

import static mousio.etcd4j.responses.EtcdKeysResponseParser.parseException;
import static mousio.etcd4j.responses.EtcdKeysResponseParser.parseResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Examples are taken out of the api.md of etcd project.
 */
public class EtcdKeysResponseParserTest {

  @Test
  public void testParseSetKey() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
        "    \"action\": \"set\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 2,\n" +
        "        \"value\": \"Hello world\"\n" +
        "    }\n" +
        "}");

    assertEquals(EtcdKeyAction.set, action.action());
    assertEquals(2, action.node().createdIndex());
    assertEquals("/message", action.node().key());
    assertEquals(2, action.node().modifiedIndex());
    assertEquals("Hello world", action.node().value());
  }

  @Test
  public void testParseGetKey() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
        "    \"action\": \"get\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 2,\n" +
        "        \"key\": \"/message\",\n" +
        "        \"modifiedIndex\": 2,\n" +
        "        \"value\": \"Hello world\"\n" +
        "    }\n" +
        "}");

    assertEquals(EtcdKeyAction.get, action.action());
    assertEquals(2, action.node().createdIndex());
    assertEquals("/message", action.node().key());
    assertEquals(2, action.node().modifiedIndex());
    assertEquals("Hello world", action.node().value());
  }

  @Test
  public void testParseChangeKey() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.set, action.action());
    assertEquals(3, action.node().createdIndex());
    assertEquals("/message", action.node().key());
    assertEquals(3, action.node().modifiedIndex());
    assertEquals("Hello etcd", action.node().value());

    assertEquals(2, action.prevNode().createdIndex());
    assertEquals("/message", action.prevNode().key());
    assertEquals(2, action.prevNode().modifiedIndex());
    assertEquals("Hello world", action.prevNode().value());
  }

  @Test
  public void testParseDeleteKey() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.delete, action.action());
    assertEquals(3, action.node().createdIndex());
    assertEquals("/message", action.node().key());
    assertEquals(4, action.node().modifiedIndex());

    assertEquals(3, action.prevNode().createdIndex());
    assertEquals("/message", action.prevNode().key());
    assertEquals(3, action.prevNode().modifiedIndex());
    assertEquals("Hello etcd", action.prevNode().value());
  }

  @Test
  public void testParseSetKeyTtl() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
        "    \"action\": \"set\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 5,\n" +
        "        \"expiration\": \"2013-12-04T12:01:21.874888581-08:00\",\n" +
        "        \"key\": \"/foo\",\n" +
        "        \"modifiedIndex\": 5,\n" +
        "        \"ttl\": 5,\n" +
        "        \"value\": \"bar\"\n" +
        "    }\n" +
        "}");

    assertEquals(EtcdKeyAction.set, action.action());
    assertEquals(5, action.node().createdIndex());
    assertEquals("/foo", action.node().key());
    assertEquals(5, action.node().modifiedIndex());
    assertEquals("bar", action.node().value());
    assertEquals(5, action.node().ttl());
    assertEquals(EtcdUtil.convertDate("2013-12-04T12:01:21.874888581-08:00"), action.node().expiration());
  }

  @Test
  public void testParseTtlExpiredException() throws Exception {
        final EtcdException exception = parseJsonException("{\n" +
            "    \"cause\": \"/foo\",\n" +
            "    \"errorCode\": 100,\n" +
            "    \"index\": 6,\n" +
            "    \"message\": \"Key Not Found\"\n" +
            "}");

      assertEquals(100, exception.errorCode());
      assertEquals(6, exception.index());
      assertEquals("/foo", exception.etcdCause());
      assertEquals("Key Not Found", exception.etcdMessage());
  }

  @Test
  public void testParseUpdateKeyTtl() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.update, action.action());
    assertEquals(5, action.node().createdIndex());
    assertEquals("/foo", action.node().key());
    assertEquals(6, action.node().modifiedIndex());
    assertEquals("bar", action.node().value());

    assertEquals(5, action.prevNode().createdIndex());
    assertEquals("/foo", action.prevNode().key());
    assertEquals(5, action.prevNode().modifiedIndex());
    assertEquals("bar", action.prevNode().value());
    assertEquals(3, action.prevNode().ttl());
    assertEquals(EtcdUtil.convertDate("2013-12-04T12:01:21.874888581-08:00"), action.prevNode().expiration());
  }

  @Test
  public void testParseCreateKey() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
        "    \"action\": \"create\",\n" +
        "    \"node\": {\n" +
        "        \"createdIndex\": 6,\n" +
        "        \"key\": \"/queue/6\",\n" +
        "        \"modifiedIndex\": 6,\n" +
        "        \"value\": \"Job1\"\n" +
        "    }\n" +
        "}");

    assertEquals(EtcdKeyAction.create, action.action());
    assertEquals(6, action.node().createdIndex());
    assertEquals("/queue/6", action.node().key());
    assertEquals(6, action.node().modifiedIndex());
    assertEquals("Job1", action.node().value());
  }

  @Test
  public void testParseGetOrderedKeys() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.get, action.action());
    assertEquals(2, action.node().createdIndex());
    assertEquals("/queue", action.node().key());
    assertEquals(2, action.node().modifiedIndex());
    assertTrue(action.node().dir());

    assertEquals(2, action.node().nodes().size());

    assertEquals(2, action.node().nodes().get(0).createdIndex());
    assertEquals(2, action.node().nodes().get(0).modifiedIndex());
    assertEquals("Job1", action.node().nodes().get(0).value());
    assertEquals("/queue/2", action.node().nodes().get(0).key());

    assertEquals(3, action.node().nodes().get(1).createdIndex());
    assertEquals(3, action.node().nodes().get(1).modifiedIndex());
    assertEquals("Job2", action.node().nodes().get(1).value());
    assertEquals("/queue/3", action.node().nodes().get(1).key());
  }


  @Test
  public void testParseExpiredDir() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.expire, action.action());
    assertEquals(8, action.node().createdIndex());
    assertEquals("/dir", action.node().key());
    assertEquals(15, action.node().modifiedIndex());

    assertEquals(8, action.prevNode().createdIndex());
    assertEquals("/dir", action.prevNode().key());
    assertEquals(17, action.prevNode().modifiedIndex());

    assertEquals(EtcdUtil.convertDate("2013-12-11T10:39:35.689275857-08:00"), action.prevNode().expiration());

    assertTrue(action.prevNode().dir());
  }

  @Test
  public void testParseCompareAndSwap() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.compareAndSwap, action.action());
    assertEquals(8, action.node().createdIndex());
    assertEquals("/foo", action.node().key());
    assertEquals(9, action.node().modifiedIndex());
    assertEquals("two", action.node().value());

    assertEquals(8, action.prevNode().createdIndex());
    assertEquals("/foo", action.prevNode().key());
    assertEquals(8, action.prevNode().modifiedIndex());
    assertEquals("one", action.prevNode().value());
  }

  @Test
  public void testParseCompareAndDelete() throws Exception {
      final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
          "}");

    assertEquals(EtcdKeyAction.compareAndDelete, action.action());
    assertEquals(8, action.node().createdIndex());
    assertEquals("/foo", action.node().key());
    assertEquals(9, action.node().modifiedIndex());

    assertEquals(8, action.prevNode().createdIndex());
    assertEquals("/foo", action.prevNode().key());
    assertEquals(8, action.prevNode().modifiedIndex());
    assertEquals("one", action.prevNode().value());
  }

  @Test
  public void testParseRecursiveGet() throws Exception {
    final EtcdKeysResponse action = parseJsonResponse("{\n" +
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
        "}");

    assertEquals(EtcdKeyAction.get, action.action());
    assertTrue(action.node().dir());

    assertEquals(1, action.node().nodes().size());

    assertEquals(2, action.node().nodes().get(0).createdIndex());
    assertEquals("/foo_dir", action.node().nodes().get(0).key());
    assertEquals(2, action.node().nodes().get(0).modifiedIndex());
    assertTrue(action.node().nodes().get(0).dir());

    assertEquals(2, action.node().nodes().get(0).nodes().get(0).createdIndex());
    assertEquals("/foo_dir/foo", action.node().nodes().get(0).nodes().get(0).key());
    assertEquals(2, action.node().nodes().get(0).nodes().get(0).modifiedIndex());
    assertEquals("bar", action.node().nodes().get(0).nodes().get(0).value());
  }

    private static EtcdKeysResponse parseJsonResponse(String json) throws Exception {
        return parseResponse(
            null,
            Unpooled.copiedBuffer(json.getBytes())
        );
    }

    private static EtcdException parseJsonException(String json) throws Exception {
        return parseException(
            null,
            Unpooled.copiedBuffer(json.getBytes())
        );
    }
}