package mousio.client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.EtcdUtil;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


public class EtcdJsonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdJsonTest.class);

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

        File file = new File("src/test/resources/test_data.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode mesos_sec = mapper.readTree(file);
        EtcdUtil.putAsJson("/etcd4j_test", mesos_sec, etcd);
    }

    @After
    public void tearDown() throws Exception {
        cleanup();

        this.etcd.close();
        this.etcd = null;
    }

    @Test
    public void testPutJson() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        File testJson = new File("src/test/resources/test_data.json");
        JsonNode toEtcd = mapper.readTree(testJson);
        EtcdUtil.putAsJson("/etcd4j_test/put-json", toEtcd, etcd);

        EtcdKeysResponse widget = etcd.get("/etcd4j_test/put-json").send().get();
        assertEquals(widget.getNode().getNodes().size(), 1);

        EtcdKeysResponse widgets = etcd.get("/etcd4j_test/put-json/widget").send().get();
        assertEquals(widgets.getNode().getNodes().size(), 5);
    }

    @Test
    public void testGetFullJson() throws Exception {
        JsonNode configJson = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        File file = new File("src/test/resources/test_data.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode originalJson = mapper.readTree(file);

        String original = EtcdUtil.jsonToString(originalJson);
        String fetched = EtcdUtil.jsonToString(configJson);

        assertEquals(original, fetched);
    }

    @Test
    public void testGetPartialJson() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        JsonNode asJson = EtcdUtil.getAsJson("/etcd4j_test/widget", etcd);
        assertEquals(asJson.at("/debug").asText(), "on");
        assertEquals(asJson.at("/window/name").asText(), "main_window");
        assertEquals(asJson.at("/text/data").size(), 2);
    }

    @Test
    public void testGetAndPut() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EtcdNettyConfig config = new EtcdNettyConfig();
        EtcdNettyClient nettyClient = new EtcdNettyClient(config, URI.create("http://localhost:4001"));
        config.setMaxFrameSize(1024 * 1024); // Desired max size
        EtcdClient client = new EtcdClient(nettyClient);

        File testJson = new File("src/test/resources/test_data.json");
        JsonNode original = mapper.readTree(testJson);

        JsonNode fromEtcd = EtcdUtil.getAsJson("/etcd4j_test", client);

        // flatten both and compare
        Map<String, Object> rootFlat = new JsonFlattener(EtcdUtil.jsonToString(original))
                .withFlattenMode(FlattenMode.MONGODB)
                .withSeparator('/')
                .flattenAsMap();

        Map<String, Object> testFlat = new JsonFlattener(EtcdUtil.jsonToString(fromEtcd))
                .withFlattenMode(FlattenMode.MONGODB)
                .withSeparator('/')
                .flattenAsMap();

        assertEquals(rootFlat.size(), testFlat.size());
        for (Map.Entry<String, Object> entry : rootFlat.entrySet()) {
            assertNotNull(testFlat.get(entry.getKey()));
        }
    }

    @Test
    public void testGetJsonArray() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        JsonNode fromEtcd = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        JsonNode arrayData = fromEtcd.at("/widget/text/data");

        assertTrue(!arrayData.isValueNode());
        assertEquals(arrayData.size(), 2);
        assertEquals(arrayData.at("/0").asText(), "Click Here");
        assertEquals(arrayData.at("/1").asText(), "Or here");
    }

    @Test
    public void testRemoveJsonArray() throws IOException, EtcdException, TimeoutException, EtcdAuthenticationException {
        String newJson = "{\n" +
                "   \"widget\":{\n" +
                "      \"text\":{\n" +
                "         \"data\":[\n" +
                "         ]\n" +
                "      }\n" +
                "   }\n" +
                "}";

        JsonNode originalConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(originalConfig.at("/widget/text/data").size(), 2);

        EtcdUtil.putAsJson("/etcd4j_test", EtcdUtil.stringToJson(newJson), etcd);
        JsonNode updatedConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(updatedConfig.at("/widget/text/data").size(), 0);
    }

    @Test
    public void testRemoveConfigurationObject() throws IOException, EtcdException, TimeoutException, EtcdAuthenticationException {
        String newJson = "{\n" +
                "   \"widget\":{\n" +
                "      \"text\":{\n" +
                "      }\n" +
                "   }\n" +
                "}";

        JsonNode originalConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(originalConfig.at("/widget/text/data").size(), 2);

        EtcdUtil.putAsJson("/etcd4j_test", EtcdUtil.stringToJson(newJson), etcd);
        JsonNode updatedConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(updatedConfig.at("/widget/text").asText(), "{}");
    }

    @Test
    public void testUpdateJsonValues() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        File testJson = new File("src/test/resources/test_data.json");
        JsonNode toEtcd = mapper.readTree(testJson);

        assertEquals(etcd.get("/etcd4j_test/widget/image/name").send().get().getNode().getValue(), "sun1");

        // update a fraction of the original json
        ObjectNode imageSubSection = (ObjectNode) toEtcd.at("/widget/image");
        imageSubSection.put("name", "moon");

        // update that fraction in etcd and check
        EtcdUtil.putAsJson("/etcd4j_test/widget/image", imageSubSection, etcd);
        assertEquals(etcd.get("/etcd4j_test/widget/image/name").send().get().getNode().getValue(), "moon");

        // verify the rest of the json is intact
        JsonNode fromEtcd = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(EtcdUtil.jsonToString(fromEtcd.at("/widget/window")),
                EtcdUtil.jsonToString(EtcdUtil.getAsJson("/etcd4j_test/widget/window", etcd)));
        assertEquals(EtcdUtil.jsonToString(fromEtcd.at("/widget/text")),
                EtcdUtil.jsonToString(EtcdUtil.getAsJson("/etcd4j_test/widget/text", etcd)));
    }

    @Test
    public void testFetchJsonValue() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        JsonNode asJson = EtcdUtil.getAsJson("/etcd4j_test/widget/text/alignment", etcd);
        assertEquals(asJson.asText(), "center");
    }

    @Test
    public void testAssignValueToObject() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
        String newJson = "{\n" +
                "   \"test\":\"value\"\n" +
                "}";

        JsonNode originalConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(originalConfig.at("/widget/window").has("test"), false);

        EtcdUtil.putAsJson("/etcd4j_test/widget/window", EtcdUtil.stringToJson(newJson), etcd);
        JsonNode updatedConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(updatedConfig.at("/widget/window").has("test"), true);
    }

    @Test
    public void testUpdateConfigurationArray() throws IOException, EtcdException, TimeoutException, EtcdAuthenticationException {
        String newJson = "[\n" +
                "  \"example-1\",\n" +
                "  \"example-2\",\n" +
                "  \"example-3\",\n" +
                "  \"example-4\"\n" +
                "]\n";

        JsonNode originalConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(originalConfig.at("/widget/text/data").size(), 2);

        EtcdUtil.putAsJson("/etcd4j_test/widget/text/data", EtcdUtil.stringToJson(newJson), etcd);
        JsonNode updatedConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(updatedConfig.at("/widget/text/data").size(), 4);
    }

    @Test
    public void testUpdateConfigurationObject() throws IOException, EtcdException, TimeoutException, EtcdAuthenticationException {
        String newJson = "{ " +
                "      \"debug\": \"off\",\n" +
                "      \"test\": \"value\"" +
                "}";

        JsonNode originalConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(originalConfig.at("/widget").has("test"), false);
        assertEquals(originalConfig.at("/widget/debug").asText(), "on");

        EtcdUtil.putAsJson("/etcd4j_test/widget", EtcdUtil.stringToJson(newJson), etcd);
        JsonNode updatedConfig = EtcdUtil.getAsJson("/etcd4j_test", etcd);
        assertEquals(updatedConfig.at("/widget").has("test"), true);
        assertEquals(updatedConfig.at("/widget/debug").asText(), "off");
        assertEquals(updatedConfig.at("/widget/test").asText(), "value");
    }

}
