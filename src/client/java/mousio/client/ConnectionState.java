package mousio.client;

import java.net.URI;

/**
 * Counts connection retries and current connection index
 */
public class ConnectionState {
  public int retryCount;
  public long startTime;
  public final URI[] uris;
  public int uriIndex;

  /**
   * Constructor
   *
   * @param uris to connect to
   */
  public ConnectionState(URI[] uris) {
    this.uris = uris;
  }
}
