package mousio.etcd4j.responses;

/**
 * Exception on etcd failures
 */
public class EtcdException extends Exception {
  private static final long serialVersionUID = -3921194095313052325L;

  public String etcdCause;
  public int errorCode;
  public Integer index;
  public String etcdMessage;

  /**
   * Constructor
   */
  public EtcdException() {
    super();
  }

  @Override
  public String getMessage() {
    return String.format("[%s]: %s%s%s",
        errorCode,
        etcdMessage,
        ((etcdCause != null) ? ", cause: " + etcdCause : ""),
        ((index != null) ? ", at index: " + index : "")
    );
  }
}