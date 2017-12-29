package mousio.client.exceptions;

/**
 * Encapsulate all SslContext builder exceptions.
 */
public class SecurityContextException extends Exception {
    public SecurityContextException(Throwable e) {
        super(e);
    }
}
