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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.flattener.KeyTransformer;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import io.netty.handler.codec.http.HttpHeaders;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdErrorCode;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author lburgazzoli
 */
public class EtcdUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(EtcdUtil.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
  }

  public static Long getHeaderPropertyAsLong(HttpHeaders headers, String key) {
    String headerValue = headers.get(key);
    if (headerValue != null) {
      try {
        return Long.parseLong(headerValue);
      } catch (Exception e) {
        LOGGER.warn("could not parse " + key + " header", e);
      }
    }

    return null;
  }

  /**
   * Transforms a Json object into String representation
   * @param jsonNode
   * @return
   */
  public static String jsonToString(JsonNode jsonNode) {
    try {
      Object json = mapper.readValue(jsonNode.toString(), Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception e) {
      LOGGER.error("unable to print json", e);
      return null;
    }
  }

  /**
   * Transforms a string representation of a json into a JsonNode object
   * @param json string representation of a json
   * @return JsonNode
   * @throws IOException
   */
  public static JsonNode stringToJson(String json) throws IOException {
    return mapper.readTree(json);
  }

  public static Date convertDate(String date) {
    return DatatypeConverter.parseDateTime(date).getTime();
  }

  /**
   * Gets the content of the key recursively as a JsonObject
   * @param path root path (i.e. /path1/path2)
   * @param etcdClient EtcdClient
   * @return JsonNode
   */
  public static JsonNode getAsJson(String path, EtcdClient etcdClient)
          throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {

    EtcdKeyGetRequest etcdKeyGetRequest = etcdClient.get(path).recursive();
    EtcdKeysResponse dataTree = etcdKeyGetRequest.send().get();

    ObjectNode jNode = JsonNodeFactory.instance.objectNode();

    if (dataTree.getNode().getNodes().isEmpty()) {
      iterateOverNodes(jNode, dataTree.getNode());
    } else {
      for (EtcdNode node : dataTree.getNode().getNodes()) {
        iterateOverNodes(jNode, node);
      }
    }

    return dotNotationToStandardJson(jNode.at(path));
  }

  /**
   * Puts the content of the Json recursively from the specified <i>path</i>
   * @param path root path (i.e. /path1/path2)
   * @param data JsonNode
   * @param etcdClient EtcdClient
   */
  public static void putAsJson(String path, JsonNode data, EtcdClient etcdClient)
          throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {

    Map<String, Object> flattened = new JsonFlattener(EtcdUtil.jsonToString(data))
            .withFlattenMode(FlattenMode.MONGODB)
            .withSeparator('/')
            .flattenAsMap();

    // clean previous data and replace it with new json structure
    try {
      etcdClient.delete(path).recursive().send().get();
    } catch (EtcdException e) {
      // interrupt always except when the previous key didn't exist
      if (EtcdErrorCode.KeyNotFound != e.errorCode) {
        throw e;
      }
    }

    // iterate over every flattened key and build the structure in etcd
    for (Map.Entry<String, Object> entry : flattened.entrySet()) {
      etcdClient.put(path + "/" + entry.getKey(), String.valueOf(entry.getValue())).send().get();
    }
  }

  /**
   * Transforms etcd format (in dot notation) to a standard Json (with arrays and primitive types)
   * @param etcdJson from etcd
   * @return standardized Json
   * @throws IOException
   */
  private static JsonNode dotNotationToStandardJson(JsonNode etcdJson) throws IOException {
    if (!etcdJson.isValueNode()) {
      String unflattened = new JsonUnflattener(jsonToString(flattenJson(etcdJson, "")))
              .withFlattenMode(FlattenMode.MONGODB)
              .withKeyTransformer(new KeyTransformer() {
                @Override
                public String transform(String s) {
                  return s.replaceAll("__DOT__", "\\.");
                }
              })
              .unflatten();
      return mapper.readTree(unflattened);
    } else {
      return etcdJson;
    }
  }

  /**
   * Iterates recursively over every node path, translating the structure of the node to a JSON structure on the parameter <i>json</i>
   * @param json JSON object to complete using the node information
   * @param node Etcd node information (recursively obtained)
   */
  private static void iterateOverNodes(ObjectNode json, EtcdNode node) {
    if (node.isDir()) {
      for(EtcdNode n: node.getNodes())
        iterateOverNodes(json, n);
    } else {
      List<String> keyPath = new ArrayList<>();

      for (String key: node.getKey().split("/")) {
        if (!key.isEmpty())
          keyPath.add(key);
      }

      ObjectNode jsonNodes = json;
      Iterator<String> iterator = keyPath.iterator();

      while (iterator.hasNext()) {
        String key = iterator.next();
        key = key.replaceAll("\\.", "__DOT__");
        if (jsonNodes.get(key)==null) {
          if (iterator.hasNext()) {
            jsonNodes = jsonNodes.putObject(key);
          } else {
            jsonNodes = jsonNodes.put(key, node.getValue());
          }
        } else {
          jsonNodes = (ObjectNode) jsonNodes.get(key);
        }
      }
    }
  }

  /**
   * Recursively, flatten all json keys and transform data types
   * @param node original json
   * @param currentPath auxiliary variable used for recursion, initially empty string
   * @return flattened json using dot notation
   */
  private static ObjectNode flattenJson(JsonNode node, String currentPath) {
    ObjectNode transformed = JsonNodeFactory.instance.objectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> next = fields.next();
      if (next.getValue().isValueNode()) {
        String path = currentPath + "." + next.getKey();
        String strValue = next.getValue().asText();
        if (NumberUtils.isCreatable(strValue)) {
          Class numberType = numberType(strValue);
          if (numberType.isAssignableFrom(Integer.class)) {
            transformed.put(path.substring(1), Integer.valueOf(strValue));
          } else if (numberType.isAssignableFrom(Long.class)) {
            transformed.put(path.substring(1), Long.valueOf(strValue));
          } else if (numberType.isAssignableFrom(Float.class)) {
            transformed.put(path.substring(1), Float.valueOf(strValue));
          } else if (numberType.isAssignableFrom(Double.class)) {
            transformed.put(path.substring(1), Double.valueOf(strValue));
          }
        } else if (booleanType(strValue)) {
          transformed.put(path.substring(1), Boolean.valueOf(strValue));
        } else if (arrayType(strValue)) {
          transformed.putArray(path.substring(1));
        } else {
          transformed.set(path.substring(1), next.getValue());
        }
      } else {
        transformed.setAll(flattenJson(next.getValue(), currentPath + "." + next.getKey()));
      }
    }

    return transformed;
  }

  /**
   * Returns the type of Number (if applicable) in class form
   * @param value Number to check
   * @return Number class type
   * @throws NumberFormatException
   */
  private static Class numberType(String value) throws NumberFormatException {
    try {
      Integer.valueOf(value);
      return Integer.class;
    } catch (NumberFormatException e) {}
    try {
      Long.valueOf(value);
      return Long.class;
    } catch (NumberFormatException e) {}
    try {
      Float.valueOf(value);
      return Float.class;
    } catch (NumberFormatException e) {}
    try {
      Double.valueOf(value);
      return Double.class;
    } catch (NumberFormatException e) {}

    // no compatible number
    throw new NumberFormatException();
  }

  /**
   * Checks if parameter value is a boolean
   * @param value string to test
   * @return if boolean
   */
  private static boolean booleanType(String value) {
    return ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value));
  }

  /**
   * Checks if parameter value is an array in string form
   * @param value string to test
   * @return is array
   */
  private static boolean arrayType(String value) {
    return "[]".equalsIgnoreCase(value);
  }
}
