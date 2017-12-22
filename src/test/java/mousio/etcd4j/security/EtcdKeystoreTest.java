package mousio.etcd4j.security;

import mousio.client.exceptions.SecurityContextException;
import mousio.etcd4j.EtcdClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class EtcdKeystoreTest {
    private final String KS_PASSWORD = "dummy_password";
    private final String KS_LOCATION = "dummy_ks";
    private final String TS_LOCATION = "dumy_ts";

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

    @Ignore
    @Test
    public void testVersion() throws SecurityContextException, URISyntaxException {
        EtcdClient etcd = new EtcdClient(SecurityContextBuilder.forKeystoreAndTruststore(
                "/keystore.jks",
                "g6fNa4JsFRJq0KgJdMqpagCrER3Jk29fVKQJtDZV",
                "/stratio/secrets/megadev/truststore.jks",
                "QPkS0cuzK8xmxSjW4yPQes8rd0ben7xy42Enuvj5"
        ), new URI("https://127.0.0.1:2379"));

        System.out.println(etcd.version());
    }

}
