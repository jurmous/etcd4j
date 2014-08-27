package mousio.jetcd;

import io.netty.handler.ssl.SslContext;
import mousio.jetcd.requests.*;
import mousio.jetcd.responses.EtcdException;
import mousio.jetcd.transport.EtcdNettyClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

/**
 * Etcd client.
 */
public class EtcdClient implements Closeable {
  private final EtcdNettyClient client;

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
   * @param baseUri    URI to create connection on
   */
  public EtcdClient(SslContext sslContext, URI... baseUri) {
    if (baseUri.length == 0) {
      baseUri = new URI[]{URI.create("https://127.0.0.1:4001")};
    }
    this.client = new EtcdNettyClient(sslContext, baseUri);
  }

  /**
   * Get the version of the Etcd server
   *
   * @return version
   */
  public String getVersion() {
    try {
      return new EtcdVersionRequest(this.client).send().get();
    } catch (IOException | EtcdException | TimeoutException e) {
      return null;
    }
  }


  /**
   * Put a key with a value
   *
   * @param key   to put
   * @param value to put on key
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest put(String key, String value) {
    return new EtcdKeyPutRequest(client, key).value(value);
  }

  /**
   * Create a dir
   *
   * @param dir to create
   * @return EtcdKeysRequest
   */
  public EtcdKeyPutRequest putDir(String dir) {
    return new EtcdKeyPutRequest(client, dir).isDir();
  }

  /**
   * Post a value to a key for in-order keys.
   *
   * @param key   to post to
   * @param value to post
   * @return EtcdKeysRequest
   */
  public EtcdKeyPostRequest post(String key, String value) {
    return new EtcdKeyPostRequest(client, key).value(value);
  }

  /**
   * Deletes a key
   *
   * @param key to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyRequest delete(String key) {
    return new EtcdKeyDeleteRequest(client, key);
  }

  /**
   * Deletes a directory
   *
   * @param dir to delete
   * @return EtcdKeysRequest
   */
  public EtcdKeyDeleteRequest deleteDir(String dir) {
    return new EtcdKeyDeleteRequest(client, dir).dir();
  }

  /**
   * Get by key
   *
   * @param key to get
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest get(String key) {
    return new EtcdKeyGetRequest(client, key);
  }

  /**
   * Get directory
   *
   * @param dir to get
   * @return EtcdKeysGetRequest
   */
  public EtcdKeyGetRequest getDir(String dir) {
    return new EtcdKeyGetRequest(client, dir).dir();
  }

  /**
   * Get all keys
   *
   * @return EtcdKeysRequest
   */
  public EtcdKeyGetRequest getAll() {
    return new EtcdKeyGetRequest(client);
  }

  @Override public void close() throws IOException {
    if (client != null) {
      client.close();
    }
  }
}