/*
 * Copyright (c) 2015, Jurriaan Mous and contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mousio.etcd4j;

import io.netty.handler.ssl.SslContext;

import javax.net.ssl.SSLContext;

public final class EtcdSecurityContext implements Cloneable {
  public static final EtcdSecurityContext NONE = new EtcdSecurityContext(null, null, null, null);

  private final SSLContext sslContext;
  private final SslContext nettySslContext;
  private final String username;
  private final String password;

  public EtcdSecurityContext(SSLContext sslContext) {
    this(sslContext, null, null, null);
  }

  public EtcdSecurityContext(SslContext sslContext) {
    this(null, sslContext, null, null);
  }

  public EtcdSecurityContext(String username, String password) {
    this(null, null, username, password);
  }

  public EtcdSecurityContext(SslContext sslContext, String username, String password) {
    this(null, sslContext, username, password);
  }

  public EtcdSecurityContext(SSLContext sslContext, String username, String password) {
    this(sslContext, null, username, password);
  }

  private EtcdSecurityContext(SSLContext sslContext, SslContext nettySslContext, String username, String password) {
    this.sslContext = sslContext;
    this.nettySslContext = nettySslContext;
    this.username = username;
    this.password = password;
  }

  public SslContext nettySslContext() {
    return this.nettySslContext;
  }

  public SSLContext sslContext() {
    return this.sslContext;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public boolean hasNettySsl() {
    return this.nettySslContext != null;
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
    return new EtcdSecurityContext(null, sslContext, null, null);
  }

  public static EtcdSecurityContext withSslContext(SSLContext sslContext) {
    return new EtcdSecurityContext(sslContext, null, null, null);
  }

  public static EtcdSecurityContext withCredential(String username, String password) {
    return new EtcdSecurityContext(null, null, username, password);
  }
}
