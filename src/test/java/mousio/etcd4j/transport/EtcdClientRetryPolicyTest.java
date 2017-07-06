package mousio.etcd4j.transport;

import java.net.URI;

import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.server.StubServer;
import mousio.client.retry.RetryNTimes;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.glassfish.grizzly.http.Method;
import org.junit.Before;
import org.junit.Test;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.custom;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.ActionSequence.sequence;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.uri;
import static org.assertj.core.api.Assertions.assertThat;

public class EtcdClientRetryPolicyTest {

    private StubServer server;
    private URI serverURI;

    private static final Action FAILURE = custom(
            response -> {
                response.getRequest()
                        .getRequest()
                        .getConnection()
                        .close();
                return response;
            });

    private static final Action SUCCESS = composite(
            stringContent("{\n" +
                    "    \"action\": \"get\",\n" +
                    "    \"node\": {\n" +
                    "        \"createdIndex\": 2,\n" +
                    "        \"key\": \"/foo\",\n" +
                    "        \"modifiedIndex\": 2,\n" +
                    "        \"value\": \"bar\"\n" +
                    "    }\n" +
                    "}"),
            ok());

    @Before
    public void start() {
        server = new StubServer().run();
        serverURI = URI.create(String.format("http://%s:%d", "localhost", server.getPort()));
    }

    @Test
    public void testSuccessWithoutRetrying() throws Exception {

        whenHttp(server)
                .match(get("/v2/keys/foo"))
                .then(SUCCESS);

        try (EtcdClient etcd = new EtcdClient(serverURI)) {

            EtcdResponsePromise<EtcdKeysResponse> promise = etcd.get("foo")
                    .setRetryPolicy(new RetryNTimes(1, 10))
                    .send();

            EtcdKeysResponse resp = promise.get();

            assertThat(resp.node.value).isEqualTo("bar");
            assertThat(promise.getException()).isNull();

        }

        verifyHttp(server).once(
                method(Method.GET),
                uri("/v2/keys/foo")
        );
    }

    @Test
    public void testFailureWithoutRetrying() throws Exception {

        whenHttp(server)
                .match(get("/v2/keys/foo"))
                .then(FAILURE);

        try (EtcdClient etcd = new EtcdClient(serverURI)) {

            EtcdResponsePromise<EtcdKeysResponse> promise = etcd.get("foo")
                    .setRetryPolicy(new RetryNTimes(1, 0))
                    .send();

            Exception err = null;

            try {
                promise.get();
            } catch (Exception e) {
                err = e;
            }

            assertThat(err).isNotNull();
            assertThat(err).isEqualTo(promise.getException());

        }

        verifyHttp(server).once(
                method(Method.GET),
                uri("/v2/keys/foo")
        );

    }

    @Test
    public void testFailureAfterRetrying() throws Exception {

        whenHttp(server)
                .match(get("/v2/keys/foo"))
                .then(FAILURE);

        try (EtcdClient etcd = new EtcdClient(serverURI)) {

            EtcdResponsePromise<EtcdKeysResponse> promise = etcd.get("foo")
                    .setRetryPolicy(new RetryNTimes(1, 3))
                    .send();

            Exception err = null;

            try {
                promise.get();
            } catch (Exception e) {
                err = e;
            }

            assertThat(err).isNotNull();
            assertThat(err).isEqualTo(promise.getException());

        }

        verifyHttp(server).times(4,
                method(Method.GET),
                uri("/v2/keys/foo")
        );
    }

    @Test
    public void testSuccessAfterRetrying() throws Exception {

        whenHttp(server)
                .match(get("/v2/keys/foo"))
                .then(sequence(FAILURE, SUCCESS));

        try (EtcdClient etcd = new EtcdClient(serverURI)) {

            EtcdResponsePromise<EtcdKeysResponse> promise = etcd.get("foo")
                    .setRetryPolicy(new RetryNTimes(1, 10))
                    .send();

            EtcdKeysResponse resp = promise.get();

            assertThat(resp.node.value).isEqualTo("bar");
            assertThat(promise.getException()).isNull();

        }

        verifyHttp(server).times(2,
                method(Method.GET),
                uri("/v2/keys/foo")
        );
    }

}