package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;

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

  protected static final String X_ETCD_CLUSTER_ID = "X-Etcd-Cluster-Id";
  protected static final String X_ETCD_INDEX = "X-Etcd-Index";
  protected static final String X_RAFT_INDEX = "X-Raft-Index";
  protected static final String X_RAFT_TERM = "X-Raft-Term";

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
   * @param headers
   * @param content to parse
   * @return EtcdResponse if found in response
   * @throws mousio.etcd4j.responses.EtcdException if exception was found in response
   * @throws java.io.IOException                   if Json parsing or parser creation fails
   */
  public static EtcdKeysResponse parse(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
    JsonParser parser = factory.createParser(new ByteBufInputStream(content));

    if (parser.nextToken() == JsonToken.START_OBJECT) {
      if (parser.nextToken() == JsonToken.FIELD_NAME
          && parser.getCurrentName().contentEquals(ACTION)) {
        return parseResponse(headers, parser);
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
    String cause = null, message = null;
    Integer errorCode = null;
    Long index = null;

    JsonToken token = parser.getCurrentToken();
    while (token != JsonToken.END_OBJECT && token != null) {
      switch (parser.getCurrentName()) {
        case CAUSE:
          cause = parser.nextTextValue();
          break;
        case MESSAGE:
          message = parser.nextTextValue();
          break;
        case ERRORCODE:
          errorCode = parser.nextIntValue(0);
          break;
        case INDEX:
          index = parser.nextLongValue(0);
          break;
        default:
          throw new JsonParseException("Unknown field in exception " + parser.getCurrentName(), parser.getCurrentLocation());
      }

      token = parser.nextToken();
    }

    return new EtcdException(errorCode, cause, message, index);
  }

  /**
   * Parses response
   *
   * @param headers
   * @param parser  to parse with
   * @return EtcdResponse
   * @throws java.io.IOException if JSON could not be parsed
   */
  private static EtcdKeysResponse parseResponse(HttpHeaders headers, JsonParser parser) throws IOException {
    String action = parser.nextTextValue();

    parser.nextToken(); // Go to the next field
    if (!parser.getCurrentName().contentEquals(NODE)) {
      throw new JsonParseException("Expecting 'node' as second field", parser.getCurrentLocation());
    }
    parser.nextToken(); // Go to the node start

    EtcdKeysResponse.EtcdNode node = parseNode(parser);

    JsonToken token = parser.nextToken(); // Go past end of object

    EtcdKeysResponse.EtcdNode prevNode = null;
    if (token == JsonToken.FIELD_NAME) {
      if (!parser.getCurrentName().contentEquals(PREVNODE)) {
        throw new JsonParseException("Expecting 'node' as second field", parser.getCurrentLocation());
      }
      parser.nextToken();
      prevNode = parseNode(parser);
      token = parser.nextToken(); // Go past end of object
    }

    String etcdClusterId = null;
    Long etcdIndex = null, raftIndex = null, raftTerm = null;
    if (headers != null) {
      etcdClusterId = headers.get(X_ETCD_CLUSTER_ID);
      etcdIndex = convertLong(headers.get(X_ETCD_INDEX));
      raftIndex = convertLong(headers.get(X_RAFT_INDEX));
      raftTerm = convertLong(headers.get(X_RAFT_TERM));
    }

    if (token == JsonToken.END_OBJECT) {
      return new EtcdKeysResponse(
          action,
          node,
          prevNode,
          etcdClusterId,
          etcdIndex,
          raftIndex,
          raftTerm);
    } else {
      throw new JsonParseException("Unexpected content after response " + token, parser.getCurrentLocation());
    }
  }

  /**
   * Converts string to long
   *
   * @param s
   * @return
   */
  private static Long convertLong(String s) {
    try {
      return Long.valueOf(s);
    } catch (NumberFormatException e) {
      return null;
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

    String key = null,
        value = null,
        expiration = null;
    long createdIndex = 0L,
        modifiedIndex = 0L,
        ttl = 0L;
    boolean dir = false;
    List<EtcdKeysResponse.EtcdNode> nodes = null;

    while (token != JsonToken.END_OBJECT && token != null) {
      switch (parser.getCurrentName()) {
        case KEY:
          key = parser.nextTextValue();
          break;
        case CREATEDINDEX:
          createdIndex = parser.nextLongValue(0);
          break;
        case MODIFIEDINDEX:
          modifiedIndex = parser.nextLongValue(0);
          break;
        case VALUE:
          value = parser.nextTextValue();
          break;
        case DIR:
          dir = parser.nextBooleanValue();
          break;
        case EXPIRATION:
          expiration = parser.nextTextValue();
          break;
        case TTL:
          ttl = parser.nextLongValue(0);
          break;
        case NODES:
          parser.nextToken();
          nodes = parseNodes(parser);
          break;
        default:
          throw new JsonParseException("Unknown field " + parser.getCurrentName(), parser.getCurrentLocation());
      }

      token = parser.nextToken();
    }

    return new EtcdKeysResponse.EtcdNode(dir, key, value, createdIndex,
        modifiedIndex, expiration, ttl, nodes);
  }

  /**
   * Converts an ISO8601 date to Java date
   *
   * @param date as string to convert
   * @return converted Date
   */
  protected static Date convertDate(String date) {
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