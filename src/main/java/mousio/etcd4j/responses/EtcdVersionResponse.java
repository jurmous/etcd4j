package mousio.etcd4j.responses;

/**
 * Etcd Keys Response
 */
public final class EtcdVersionResponse {

  public final String server;
  public final String cluster;

  /**
   * Protected constructor
   *
   * @param server
   * @param cluster
   */
  EtcdVersionResponse(
    final String server,
    final String cluster) {
    this.server = server;
    this.cluster = cluster;
  }
}