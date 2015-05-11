package mousio.client.util;

import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SRV2URIsTest {

  @Test
  public void testFromDNSName() throws Exception {
    URI[] uris = SRV2URIs.fromDNSName("etcd4jtest.mousio.org");

    List<URI> toFind = new ArrayList<>(Arrays.asList(
        URI.create("http://test1.nl:4001"),
        URI.create("http://test2.nl:4001"),
        URI.create("http://test3.nl:4001")
    ));

    // Order is maybe not the same so walk till all are matched
    for (URI uri : uris) {
      if (toFind.contains(uri)) {
        toFind.remove(uri);
      } else {
        fail(uri + " not found in expected list");
      }
    }

    assertTrue(toFind.isEmpty());
  }
}