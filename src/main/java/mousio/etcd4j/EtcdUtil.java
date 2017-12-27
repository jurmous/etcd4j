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
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;

/**
 * @author lburgazzoli
 */
public class EtcdUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(EtcdUtil.class);

  private static final ObjectMapper mapper = new ObjectMapper();

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
}
