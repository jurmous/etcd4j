package mousio.etcd4j;

import io.netty.handler.ssl.SslContext;

public final class EtcdSecurityContext implements Cloneable {
  public static final EtcdSecurityContext NONE = new EtcdSecurityContext(null, null, null);

  private final SslContext sslContext;
  private final String username;
  private final String password;

  public EtcdSecurityContext(SslContext sslContext) {
    this(sslContext, null, null);
  }

  public EtcdSecurityContext(String username, String password) {
    this(null, username, password);
  }

  public EtcdSecurityContext(SslContext sslContext, String username, String password) {
    this.sslContext = sslContext;
    this.username = username;
    this.password = password;
  }

  public SslContext sslContext() {
    return this.sslContext;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public boolean hasSsl() {
    return this.sslContext != null;
  }

  public boolean hasCredentials() {
    return this.username != null && !this.username.trim().isEmpty()
      && this.password != null && !this.password.trim().isEmpty();
  }

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  @Override
  public EtcdSecurityContext clone() {
    try {
      return (EtcdSecurityContext) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  public static EtcdSecurityContext withSslContext(SslContext sslContext) {
    return new EtcdSecurityContext(sslContext, null, null);
  }

  public static EtcdSecurityContext withCredential(String username, String password) {
    return new EtcdSecurityContext(null, username, password);
  }
}
