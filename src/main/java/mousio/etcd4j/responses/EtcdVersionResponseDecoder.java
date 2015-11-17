package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

/**
 * Parses the JSON response for key responses
 */
public class EtcdVersionResponseDecoder extends AbstractJsonResponseDecoder<EtcdVersionResponse> {
  public static final EtcdVersionResponseDecoder INSTANCE = new EtcdVersionResponseDecoder();

  private static final JsonFactory factory = new JsonFactory();

  private static final String ETCD_SERVER  = "etcdserver";
  private static final String ETCD_CLUSTER = "etcdcluster";

  /**
   * Parses the Json content of the Etcd Response
   *
   * @param headers
   * @param parser Json parser
   * @return EtcdResponse if found in response
   * @throws EtcdException if exception was found in response
   * @throws IOException                   if Json parsing or parser creation fails
   */
  public EtcdVersionResponse decodeJson(HttpHeaders headers, JsonParser parser) throws EtcdException, IOException {
    if (parser.nextToken() == JsonToken.START_OBJECT) {

      final String etcdserver;
      final String etcdcluster;

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