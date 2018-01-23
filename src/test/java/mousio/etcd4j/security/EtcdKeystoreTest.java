package mousio.etcd4j.security;

import mousio.client.exceptions.SecurityContextException;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.TestTimedOutException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;

@Ignore
public class EtcdKeystoreTest {
    private static String KEYSTORE_PATH = "keystore.jks";
    private static final String KEYSTORE_PASS = "password";
    private static String TRUSTSTORE_PATH = "truststore.jks";
    private static final String TRUSTSTORE_PASS = "password";

    private static final String SECURED_ETCD_SERVICE = "https://127.0.0.1:2378";

    private static final String KS_PASSWORD = "dummy_password";
    private static final String KS_LOCATION = "dummy_ks";
    private static final String TS_LOCATION = "dummy_ts";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    static {
        KEYSTORE_PATH = new File("src/test/resources/certs/keystore.jks").getAbsolutePath();
        TRUSTSTORE_PATH = new File("src/test/resources/certs/truststore.jks").getAbsolutePath();
    }

    protected void cleanup(EtcdClient etcd) {
        try {
            for (EtcdKeysResponse.EtcdNode node: etcd.getAll().send().get().getNode().getNodes()) {
                if (node.isDir()) {
                    etcd.deleteDir(node.key).recursive().send().get();
                } else {
                    etcd.delete(node.key).send().get();
                }
            }
        } catch (Exception e) {}
    }

    @Test(expected = SecurityContextException.class)
    public void testInvalidKeystoreLocation() throws SecurityContextException {
        SecurityContextBuilder.forKeystore(KS_LOCATION, KS_PASSWORD);
    }

    @Test(expected = SecurityContextException.class)
    public void testInvalidTruststoreLocation() throws SecurityContextException {
        SecurityContextBuilder.forKeystoreAndTruststore(KS_LOCATION, KS_PASSWORD, TS_LOCATION, KS_PASSWORD);
    }

    @Test(expected = SecurityContextException.class)
    public void testInvalidKeystoreFormat() throws UnsupportedEncodingException, SecurityContextException {
        InputStream stream = new ByteArrayInputStream("bad_format_keystore".getBytes(StandardCharsets.UTF_8.name()));
        SecurityContextBuilder.forKeystore(stream, KS_PASSWORD, "SunX509");
    }

    @Test(expected = SecurityContextException.class)
    public void testInvalidTruststoreFormat() throws UnsupportedEncodingException, SecurityContextException {
        InputStream stream = new ByteArrayInputStream("bad_format_keystore".getBytes(StandardCharsets.UTF_8.name()));
        SecurityContextBuilder.forKeystoreAndTruststore(stream, KS_PASSWORD, stream, KS_PASSWORD, "SunX509");
    }

    @Test
    public void testSslCliAgainstSslEtcd() throws SecurityContextException, URISyntaxException, IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
        // expected to work only on a secured etcd
        EtcdClient etcd = new EtcdClient(SecurityContextBuilder.forKeystoreAndTruststore(
                KEYSTORE_PATH,
                KEYSTORE_PASS,
                TRUSTSTORE_PATH,
                TRUSTSTORE_PASS
        ), new URI(SECURED_ETCD_SERVICE));

        etcd.put("/test", "1234").send().get();
        assertNotNull(etcd.version());
        cleanup(etcd);
    }

    @Test
    public void testSslCliAgainstRegularEtcd() throws SecurityContextException, URISyntaxException, IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
        expectedEx.expect(IOException.class);
        expectedEx.expectMessage(matchesRegex("DecoderException: io.netty.handler.ssl.NotSslRecordException"));

        EtcdClient etcd = new EtcdClient(SecurityContextBuilder.forKeystoreAndTruststore(
                KEYSTORE_PATH,
                KEYSTORE_PASS,
                TRUSTSTORE_PATH,
                TRUSTSTORE_PASS
        ));

        // expected not to work when using certificates against a non-secured etcd
        etcd.put("/test", "1234").send().get();
    }

    @Test(timeout = 1000)
    public void testCliAgainstSslEtcd() throws URISyntaxException, IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
        expectedEx.expect(TestTimedOutException.class);
        EtcdClient etcd = new EtcdClient(new URI(SECURED_ETCD_SERVICE));
        etcd.put("/test", "1234").send().get();
    }

    private Matcher<String> matchesRegex(final String message) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {}

            @Override
            protected boolean matchesSafely(final String item) {
                return item.contains(message);
            }
        };
    }

}
