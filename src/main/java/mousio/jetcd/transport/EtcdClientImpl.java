package mousio.jetcd.transport;

import mousio.jetcd.promises.EtcdResponsePromise;
import mousio.jetcd.requests.EtcdRequest;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for Etcd client implementations
 */
public interface EtcdClientImpl extends Closeable {

  /**
   * Sends a request to the server
   *
   * @param request to send
   * @param <R>     Type of response
   * @return A Promise
   * @throws java.io.IOException if IO failure while sending
   */
  public <R> EtcdResponsePromise<R> send(EtcdRequest<R> request) throws IOException;

  @Override
  public void close();
}