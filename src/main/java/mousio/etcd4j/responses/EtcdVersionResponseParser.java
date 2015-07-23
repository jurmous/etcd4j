package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

/**
 * Parses the JSON response for key responses
 */
public class EtcdVersionResponseParser {
  private static final JsonFactory factory = new JsonFactory();

  private static final String ETCD_SERVER  = "etcdserver";
  private static final String ETCD_CLUSTER = "etcdcluster";

  /**
   * Parses the Json content of the Etcd Response
   *
   * @param headers
   * @param content to parse
   * @return EtcdResponse if found in response
   * @throws EtcdException if exception was found in response
   * @throws IOException                   if Json parsing or parser creation fails
   */
  public static EtcdVersionResponse parse(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
    JsonParser parser = factory.createParser(new ByteBufInputStream(content));

    if (parser.nextToken() == JsonToken.START_OBJECT) {
      String etcdserver = null;
      String etcdcluster = null;

      if (parser.nextToken() == JsonToken.FIELD_NAME && parser.getCurrentName().contentEquals(ETCD_SERVER)) {
        etcdserver = parser.nextTextValue();
      } else {
        throw new JsonParseException("Expecting 'etcdserver' as first field", parser.getCurrentLocation());
      }

      if (parser.nextToken() == JsonToken.FIELD_NAME && parser.getCurrentName().contentEquals(ETCD_CLUSTER)) {
        etcdcluster = parser.nextTextValue();
      } else {
        throw new JsonParseException("Expecting 'etcdcluster' as second field", parser.getCurrentLocation());
      }

      return new EtcdVersionResponse(etcdserver, etcdcluster);
    }

    return null;
  }
}