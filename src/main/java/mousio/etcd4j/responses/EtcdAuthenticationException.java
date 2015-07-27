package mousio.etcd4j.responses;

/**
 * Exception on etcd failures
 */
public class EtcdAuthenticationException extends Exception {

  /**
   * Constructor
   * @param message
   */
  public EtcdAuthenticationException(
    final String message) {
    super(message);
  }
}