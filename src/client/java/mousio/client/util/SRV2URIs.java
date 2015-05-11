package mousio.client.util;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Utility class to convert SRV records to array of URIs
 */
public final class SRV2URIs {

  /**
   * Convert given DNS SRV address to array of URIs
   *
   * @param service complete DNS name to resolve to URIs
   * @return Array of URIs
   * @throws NamingException if DNS name was invalid
   */
  public static URI[] fromService(String service) throws NamingException {
    return fromDNSName("_etcd-server._tcp." + service);
  }

  /**
   * Convert given DNS SRV address to array of URIs
   *
   * @param srvName complete DNS name to resolve to URIs
   * @return Array of URIs
   * @throws NamingException if DNS name was invalid
   */
  public static URI[] fromDNSName(String srvName) throws NamingException {
    List<URI> uris = new ArrayList<>();
    Hashtable<String, String> env = new Hashtable<>();
    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
    env.put("java.naming.provider.url", "dns:");

    DirContext ctx = new InitialDirContext(env);
    Attributes attributes = ctx.getAttributes(srvName, new String[]{"SRV"});
    NamingEnumeration<? extends Attribute> records = attributes.getAll();

    while (records.hasMore()) {
      Attribute next = records.next();

      @SuppressWarnings("unchecked")
      NamingEnumeration<String> values = (NamingEnumeration<String>) next.getAll();
      while (values.hasMore()) {
        String dns = values.next();
        String[] split = dns.split(" ");
        String port = split[2];
        String host = split[3];
        if (host.endsWith(".")) {
          host = host.substring(0, host.length() - 1);
        }
        URI uri = URI.create("http://" + host + ":" + port);
        uris.add(uri);
      }
    }
    return uris.toArray(new URI[uris.size()]);
  }
}
