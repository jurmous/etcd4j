package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Parses the JSON response for key responses
 */
public class EtcdKeysResponseParser {
  private static final JsonFactory factory = new JsonFactory();

  private static final String ACTION = "action";
  private static final String NODE = "node";
  private static final String PREVNODE = "prevNode";

  private static final String KEY = "key";
  private static final String DIR = "dir";
  private static final String CREATEDINDEX = "createdIndex";
  private static final String MODIFIEDINDEX = "modifiedIndex";
  private static final String VALUE = "value";
  private static final String EXPIRATION = "expiration";
  private static final String TTL = "ttl";
  private static final String NODES = "nodes";

  private static final String CAUSE = "cause";
  private static final String ERRORCODE = "errorCode";
  private static final String MESSAGE = "message";
  private static final String INDEX = "index";

  /**
   * Parses the Json content of the Etcd Response
   *
   * @param content to parse
   * @return EtcdResponse if found in response
   * @throws mousio.etcd4j.responses.EtcdException if exception was found in response
   * @throws java.io.IOException                   if Json parsing or parser creation fails
   */
  public static EtcdKeysResponse parse(ByteBuf content) throws EtcdException, IOException {
    JsonParser parser = factory.createParser(new ByteBufInputStream(content));

    if (parser.nextToken() == JsonToken.START_OBJECT) {
      if (parser.nextToken() == JsonToken.FIELD_NAME
          && parser.getCurrentName().contentEquals(ACTION)) {
        return parseResponse(parser);
      } else {
        throw parseException(parser);
      }
    }

    return null;
  }

  /**
   * Parses an EtcdException
   *
   * @param parser to parse with
   * @return EtcdException
   * @throws java.io.IOException IOException
   */
  private static EtcdException parseException(JsonParser parser) throws IOException {
    EtcdException exception = new EtcdException();

    JsonToken token = parser.getCurrentToken();
    while (token != JsonToken.END_OBJECT && token != null) {
      switch (parser.getCurrentName()) {
        case CAUSE:
          exception.etcdCause = parser.nextTextValue();
          break;
        case MESSAGE:
          exception.etcdMessage = parser.nextTextValue();
          break;
        case ERRORCODE:
          exception.errorCode = parser.nextIntValue(0);
          break;
        case INDEX:
          exception.index = parser.nextIntValue(0);
          break;
        default:
          throw new JsonParseException("Unknown field in exception " + parser.getCurrentName(), parser.getCurrentLocation());
      }

      token = parser.nextToken();
    }

    return exception;
  }

  /**
   * Parses response
   *
   * @param parser to parse with
   * @return EtcdResponse
   * @throws java.io.IOException if JSON could not be parsed
   */
  private static EtcdKeysResponse parseResponse(JsonParser parser) throws IOException {
    String action = parser.nextTextValue();

    parser.nextToken(); // Go to the next field
    if (!parser.getCurrentName().contentEquals(NODE)) {
      throw new JsonParseException("Expecting 'node' as second field", parser.getCurrentLocation());
    }
    parser.nextToken(); // Go to the node start

    EtcdKeysResponse response = new EtcdKeysResponse(action, parseNode(parser));
    JsonToken token = parser.nextToken(); // Go past end of object

    if (token == JsonToken.FIELD_NAME) {
      if (!parser.getCurrentName().contentEquals(PREVNODE)) {
        throw new JsonParseException("Expecting 'node' as second field", parser.getCurrentLocation());
      }
      parser.nextToken();
      response.prevNode = parseNode(parser);
      token = parser.nextToken(); // Go past end of object
    }

    if (token == JsonToken.END_OBJECT) {
      return response;
    } else {
      throw new JsonParseException("Unexpected content after response " + token, parser.getCurrentLocation());
    }
  }

  /**
   * Parses a Etcd Node
   *
   * @param parser to use
   * @return Parsed EtcdNode
   * @throws java.io.IOException if JSON content could not be parsed
   */
  private static EtcdKeysResponse.EtcdNode parseNode(JsonParser parser) throws IOException {
    if (parser.getCurrentToken() != JsonToken.START_OBJECT) {
      throw new JsonParseException("Expecting object at start of node description", parser.getCurrentLocation());
    }

    JsonToken token = parser.nextToken();
    EtcdKeysResponse.EtcdNode node = new EtcdKeysResponse.EtcdNode();

    while (token != JsonToken.END_OBJECT && token != null) {
      switch (parser.getCurrentName()) {
        case KEY:
          node.key = parser.nextTextValue();
          break;
        case CREATEDINDEX:
          node.createdIndex = parser.nextLongValue(0);
          break;
        case MODIFIEDINDEX:
          node.modifiedIndex = parser.nextLongValue(0);
          break;
        case VALUE:
          node.value = parser.nextTextValue();
          break;
        case DIR:
          node.dir = parser.nextBooleanValue();
          break;
        case EXPIRATION:
          node.expiration = convertDate(parser.nextTextValue());
          break;
        case TTL:
          node.ttl = parser.nextLongValue(0);
          break;
        case NODES:
          parser.nextToken();
          node.nodes = parseNodes(parser);
          break;
        default:
          throw new JsonParseException("Unknown field " + parser.getCurrentName(), parser.getCurrentLocation());
      }

      token = parser.nextToken();
    }

    return node;
  }

  /**
   * Converts an ISO8601 date to Java date
   *
   * @param date as string to convert
   * @return converted Date
   * @throws IOException if date was of wrong type
   */
  protected static Date convertDate(String date) throws IOException {
    return DatatypeConverter.parseDateTime(date).getTime();
  }

  /**
   * Parses an array with node descriptions
   *
   * @param parser to parse with
   * @return List of EtcdNodes
   * @throws IOException if JSON content could not be parsed
   */
  private static List<EtcdKeysResponse.EtcdNode> parseNodes(JsonParser parser) throws IOException {
    if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
      throw new JsonParseException("Expecting an array of nodes", parser.getCurrentLocation());
    }
    List<EtcdKeysResponse.EtcdNode> nodes = new ArrayList<>();

    JsonToken token = parser.nextToken();
    while (token != JsonToken.END_ARRAY && token != null) {
      nodes.add(parseNode(parser));

      token = parser.nextToken();
    }

    return nodes;
  }
}