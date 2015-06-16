package mousio.etcd4j.responses;

/**
 * Exception on etcd failures
 */
public class EtcdException extends Exception {
  private static final long serialVersionUID = -3921194095313052325L;

  public final String etcdCause;
  public final int errorCode;
  public final Long index;
  public final String etcdMessage;

  /**
   * Constructor
   *
   * @param errorCode
   * @param cause
   * @param message
   * @param index
   */
  protected EtcdException(
      final int errorCode,
      final String cause,
      final String message,
      final Long index) {

    this.errorCode = errorCode;
    this.etcdCause = cause;
    this.etcdMessage = message;
    this.index = index;
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

  @Override
  public String toString() {
    return getMessage();
  }
}