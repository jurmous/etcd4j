package mousio.etcd4j;

import io.netty.handler.ssl.SslContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import mousio.client.retry.RetryPolicy;
import mousio.client.retry.RetryWithExponentialBackOff;
import mousio.etcd4j.requests.EtcdKeyDeleteRequest;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.requests.EtcdKeyPostRequest;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import mousio.etcd4j.requests.EtcdKeyRequest;
import mousio.etcd4j.requests.EtcdVersionRequest;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.transport.EtcdClientImpl;
import mousio.etcd4j.transport.EtcdNettyClient;

/**
 * Etcd client.
 */
public class EtcdClient implements Closeable {
  private final EtcdClientImpl client;
  private RetryPolicy retryHandler = new RetryWithExponentialBackOff(20, -1, 10000);

  /**
   * Constructor
   *
   * @param baseUri URI to create connection on
   */
  public EtcdClient(URI... baseUri) {
    this(null, baseUri);
  }

  /**
   * Constructor
   *
   * @param sslContext context for Ssl connections
   * @param baseUri URI to create connection on
   */
  public EtcdClient(SslContext sslContext, URI... baseUri) {
    if (baseUri.length == 0) {
      baseUri = new URI[] { URI.create("https://127.0.0.1:4001") };
    }
    this.client = new EtcdNettyClient(sslContext, baseUri);
  }

  public EtcdClient(EtcdClientImpl etcdClientImpl) {
    this.client = etcdClientImpl;
  }

  /**
   * Get the version of the Etcd server
   *
   * @return version
   */
  public String getVersion() {
    try {
      return new EtcdVersionRequest(this.client, retryHandler).send().get();
    } catch (IOException | EtcdException | TimeoutException e) {
      return null;
    }
  }

  /**
   * Put a key with a value
   *
   * @param key to put
   * @param value to put on key
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest put(String key, String value) {
    return new EtcdKeyPutRequest(client, key, retryHandler).value(value);
  }

  /**
   * Create a dir
   *
   * @param dir to create
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest putDir(String dir) {
    return new EtcdKeyPutRequest(client, dir, retryHandler).isDir();
  }

  /**
   * Post a value to a key for in-order keys.
   *
   * @param key to post to
   * @param value to post
   * @return EtcdKeysRequest
   */
  public EtcdKeyPostRequest post(String key, String value) {
    return new EtcdKeyPostRequest(client, key, retryHandler).value(value);
  }

  /**
   * Deletes a key
   *
   * @param key to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyRequest delete(String key) {
    return new EtcdKeyDeleteRequest(client, key, retryHandler);
  }

  /**
   * Deletes a directory
   *
   * @param dir to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyDeleteRequest deleteDir(String dir) {
    return new EtcdKeyDeleteRequest(client, dir, retryHandler).dir();
  }

  /**
   * Get by key
   *
   * @param key to get
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest get(String key) {
    return new EtcdKeyGetRequest(client, key, retryHandler);
  }

  /**
   * Get directory
   *
   * @param dir to get
   * @return EtcdKeysGetRequest
   */
  public EtcdKeyGetRequest getDir(String dir) {
    return new EtcdKeyGetRequest(client, dir, retryHandler).dir();
  }

  /**
   * Get all keys
   *
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest getAll() {
    return new EtcdKeyGetRequest(client, retryHandler);
  }

  @Override
  public void close() throws IOException {
    if (client != null) {
      client.close();
    }
  }

  /**
   * Set the retry handler. Default is an exponential backoff with start of 20ms.
   *
   * @param retryHandler to set
   */
  public void setRetryHandler(RetryPolicy retryHandler) {
    this.retryHandler = retryHandler;
  }

  public static class Builder extends AbstractEtcdClientBuilder<Builder> {

    public EtcdNettyClient.Builder usingNetty() {
      EtcdNettyClient.Builder builder = new EtcdNettyClient.Builder();
      if (sslContext != null) {
        builder.usingSslContext(sslContext);
      }
      return builder.usingURIs(uris);
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public EtcdClient build() {
      return new EtcdClient(sslContext, uris);
    }
  }

  public static abstract class AbstractEtcdClientBuilder<T extends AbstractEtcdClientBuilder<T>> {

    protected SslContext sslContext;
    protected URI[] uris = new URI[] { URI.create("https://127.0.0.1:4001") };

    protected abstract T self();

    public abstract EtcdClient build();

    public T usingSslContext(SslContext sslContext) {
      if (sslContext == null) {
        throw new IllegalArgumentException("Provided sslContext cannot be null");
      }
      this.sslContext = sslContext;
      return self();
    }

    public T usingURIs(URI... uris) {
      if (uris == null || uris.length == 0) {
        throw new IllegalArgumentException("Provided URIs cannot be null or empty");
      }
      this.uris = uris;
      return self();
    }
  }
}