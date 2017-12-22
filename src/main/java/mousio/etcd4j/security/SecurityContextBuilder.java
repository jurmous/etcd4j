package mousio.etcd4j.security;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import mousio.client.exceptions.SecurityContextException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Easy-to-use SslContext builder for etcd clients
 */
public final class SecurityContextBuilder {
    public static final String KEYSTORE_JKS = "JKS";

    /**
     * Builds SslContext using a protected keystore file. Adequate for non-mutual TLS connections.
     *
     * @param keystorePath Path for keystore file
     * @param keystorePassword Password for protected keystore file
     * @return SslContext ready to use
     * @throws SecurityContextException for any troubles building the SslContext
     */
    public static SslContext forKeystore(String keystorePath, String keystorePassword)
            throws SecurityContextException {
        return forKeystore(keystorePath, keystorePassword, "SunX509");
    }

    /**
     * Builds SslContext using protected keystore file overriding default key manger algorithm. Adequate for non-mutual TLS connections.
     *
     * @param keystorePath Path for keystore file
     * @param keystorePassword Password for protected keystore file
     * @param keyManagerAlgorithm Algorithm for keyManager used to process keystorefile
     * @return SslContext ready to use
     * @throws SecurityContextException for any troubles building the SslContext
     */
    public static SslContext forKeystore(String keystorePath, String keystorePassword, String keyManagerAlgorithm)
            throws SecurityContextException {

        try {
            return forKeystore(new FileInputStream(keystorePath), keystorePassword, keyManagerAlgorithm);
        } catch (Exception e) {
            throw new SecurityContextException(e);
        }
    }

    /**
     * Builds SslContext using protected keystore file overriding default key manger algorithm. Adequate for non-mutual TLS connections.
     *
     * @param keystore Keystore inputstream (file, binaries, etc)
     * @param keystorePassword Password for protected keystore file
     * @param keyManagerAlgorithm Algorithm for keyManager used to process keystorefile
     * @return SslContext ready to use
     * @throws SecurityContextException for any troubles building the SslContext
     */
    public static SslContext forKeystore(InputStream keystore, String keystorePassword, String keyManagerAlgorithm)
            throws SecurityContextException {

        try {
            final KeyStore ks = KeyStore.getInstance(KEYSTORE_JKS);
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);

            ks.load(keystore, keystorePassword.toCharArray());
            kmf.init(ks, keystorePassword.toCharArray());

            SslContextBuilder ctxBuilder = SslContextBuilder.forClient().keyManager(kmf);
            return ctxBuilder.build();
        } catch (Exception e) {
            throw new SecurityContextException(e);
        }
    }

    /**
     * Builds SslContext using protected keystore and truststores. Adequate for mutual TLS connections.
     * @param keystorePath Path for keystore file
     * @param keystorePassword Password for protected keystore file
     * @param truststorePath Path for truststore file
     * @param truststorePassword Password for protected truststore file
     * @return SslContext ready to use
     * @throws SecurityContextException
     */
    public static SslContext forKeystoreAndTruststore(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword)
            throws SecurityContextException {
        return forKeystoreAndTruststore(keystorePath, keystorePassword, truststorePath, truststorePassword, "SunX509");
    }

    /**
     * Builds SslContext using protected keystore and truststores. Adequate for mutual TLS connections.
     * @param keystorePath Path for keystore file
     * @param keystorePassword Password for protected keystore file
     * @param truststorePath Path for truststore file
     * @param truststorePassword Password for protected truststore file
     * @param keyManagerAlgorithm Algorithm for keyManager used to process keystorefile
     * @return SslContext ready to use
     * @throws SecurityContextException
     */
    public static SslContext forKeystoreAndTruststore(String keystorePath, String keystorePassword, String truststorePath, String truststorePassword, String keyManagerAlgorithm)
            throws SecurityContextException {

        try {
            return forKeystoreAndTruststore(new FileInputStream(keystorePath), keystorePassword, new FileInputStream(truststorePath), truststorePassword, keyManagerAlgorithm);
        } catch (Exception e) {
            throw new SecurityContextException(e);
        }
    }

    /**
     * Builds SslContext using protected keystore and truststores, overriding default key manger algorithm. Adequate for mutual TLS connections.
     * @param keystore Keystore inputstream (file, binaries, etc)
     * @param keystorePassword Password for protected keystore file
     * @param truststore Truststore inputstream (file, binaries, etc)
     * @param truststorePassword Password for protected truststore file
     * @param keyManagerAlgorithm Algorithm for keyManager used to process keystorefile
     * @return SslContext ready to use
     * @throws SecurityContextException
     */
    public static SslContext forKeystoreAndTruststore(InputStream keystore, String keystorePassword, InputStream truststore, String truststorePassword, String keyManagerAlgorithm)
            throws SecurityContextException {
        try {
            final KeyStore ks = KeyStore.getInstance(KEYSTORE_JKS);
            final KeyStore ts = KeyStore.getInstance(KEYSTORE_JKS);

            final KeyManagerFactory keystoreKmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            final TrustManagerFactory truststoreKmf = TrustManagerFactory.getInstance(keyManagerAlgorithm);

            ks.load(keystore, keystorePassword.toCharArray());
            ts.load(truststore, truststorePassword.toCharArray());

            keystoreKmf.init(ks, keystorePassword.toCharArray());
            truststoreKmf.init(ts);

            SslContextBuilder ctxBuilder = SslContextBuilder.forClient().keyManager(keystoreKmf);
            ctxBuilder.trustManager(truststoreKmf);

            return ctxBuilder.build();
        } catch (Exception e) {
            throw new SecurityContextException(e);
        }
    }
}
