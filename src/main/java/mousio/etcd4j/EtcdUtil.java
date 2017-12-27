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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import io.netty.handler.codec.http.HttpHeaders;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
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

  public static String printJson(JsonNode jsonNode) {
    try {
      Object json = mapper.readValue(jsonNode.toString(), Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception e) {
      LOGGER.error("unable to print json", e);
      return null;
    }
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

    for (EtcdNode node: dataTree.getNode().getNodes()) {
      iterateOverNodes(jNode, node);
    }

    return jNode.at(path);
  }

  /**
   * Puts the content of the Json recursively from the specified <i>path</i>
   * @param path root path (i.e. /path1/path2)
   * @param data JsonNode
   * @param etcdClient EtcdClient
   */
  public static void putAsJson(String path, JsonNode data, EtcdClient etcdClient)
          throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {

    Map<String, Object> flattened = new JsonFlattener(EtcdUtil.printJson(data))
            .withFlattenMode(FlattenMode.MONGODB)
            .withSeparator('/')
            .flattenAsMap();

    for (Map.Entry<String, Object> entry : flattened.entrySet()) {
      etcdClient.put(path + "/" + entry.getKey(), String.valueOf(entry.getValue())).send().get();
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
}
