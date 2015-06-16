package mousio.etcd4j.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exception on etcd failures
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class EtcdException extends Exception {
    private static final long serialVersionUID = -3921194095313052325L;

    private String etcdCause;
    private Integer errorCode;
    private Long index;
    private String etcdMessage;

    public EtcdException(
        @JsonProperty("errorCode") final Integer errorCode,
        @JsonProperty("cause") final String cause,
        @JsonProperty("message") final String message,
        @JsonProperty("index") final Long index) {

        this.errorCode = errorCode;
        this.etcdCause = cause;
        this.etcdMessage = message;
        this.index = index;
    }

    public int errorCode() {
        return errorCode;
    }

    public long index() {
        return index;
    }

    public String etcdMessage() {
        return etcdMessage;
    }

    public String etcdCause() {
        return etcdCause;
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