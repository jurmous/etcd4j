/*
 * Copyright (c) 2015, contributors as indicated by the @author tags.
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
package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 * 
 * Parses the JSON response for storage stats responses
 */
public class EtcdStoreStatsResponseDecoder extends AbstractJsonResponseDecoder<EtcdStoreStatsResponse> {
  public static final EtcdStoreStatsResponseDecoder INSTANCE = new EtcdStoreStatsResponseDecoder();

  private static final JsonFactory factory = new JsonFactory();

  public static final String COMPAREANDSWAPFAIL    = "compareAndSwapFail";
  public static final String COMPAREANDSWAPSUCCESS = "compareAndSwapSuccess";
  public static final String CREATEFAIL            = "createFail";
  public static final String CREATESUCCESS         = "createSuccess";
  public static final String DELETEFAIL            = "deleteFail";
  public static final String DELETESUCCESS         = "deleteSuccess";
  public static final String EXPIRECOUNT           = "expireCount";
  public static final String GETSFAIL              = "getsFail";
  public static final String GETSSUCCESS           = "getsSuccess";
  public static final String SETSFAIL              = "setsFail";
  public static final String SETSSUCCESS           = "setsSuccess";
  public static final String UPDATEFAIL            = "updateFail";
  public static final String UPDATESUCCESS         = "updateSuccess";
  public static final String WATCHERS              = "watchers";

  /**
   * Parses the Json content of the Etcd Response
   *
   * @param headers
   * @param parser Json parser
   * @return EtcdResponse   if found in response
   * @throws EtcdException  if exception was found in response
   * @throws IOException    if Json parsing or parser creation fails
   */
  @Override
  public EtcdStoreStatsResponse decodeJson(HttpHeaders headers, JsonParser parser) throws EtcdException, IOException {
    long compareAndSwapFail = 0;
    long compareAndSwapSuccess = 0;
    long createFail = 0;
    long createSuccess = 0;
    long deleteFail = 0;
    long deleteSuccess = 0;
    long expireCount = 0;
    long getsFail = 0;
    long getsSuccess = 0;
    long setsFail = 0;
    long setsSuccess = 0;
    long updateFail = 0;
    long updateSuccess = 0;
    long watchers = 0;

    if (parser.nextToken() == JsonToken.START_OBJECT) {

      JsonToken token = parser.nextToken();
      while (token != JsonToken.END_OBJECT && token != null) {

        switch (parser.getCurrentName()) {
          case COMPAREANDSWAPFAIL:
            compareAndSwapFail = parser.nextLongValue(0);
            break;
          case COMPAREANDSWAPSUCCESS:
            compareAndSwapSuccess = parser.nextLongValue(0);
            break;
          case CREATEFAIL:
            createFail = parser.nextLongValue(0);
            break;
          case CREATESUCCESS:
            createSuccess = parser.nextLongValue(0);
            break;
          case DELETEFAIL:
            deleteFail = parser.nextLongValue(0);
            break;
          case DELETESUCCESS:
            deleteSuccess = parser.nextLongValue(0);
            break;
          case EXPIRECOUNT:
            expireCount = parser.nextLongValue(0);
            break;
          case GETSFAIL:
            getsFail = parser.nextLongValue(0);
            break;
          case GETSSUCCESS:
            getsSuccess = parser.nextLongValue(0);
            break;
          case SETSFAIL:
            setsFail = parser.nextLongValue(0);
            break;
          case SETSSUCCESS:
            setsSuccess = parser.nextLongValue(0);
            break;
          case UPDATEFAIL:
            updateFail = parser.nextLongValue(0);
            break;
          case UPDATESUCCESS:
            updateSuccess = parser.nextLongValue(0);
            break;
          case WATCHERS:
            watchers = parser.nextLongValue(0);
            break;
        }

        token = parser.nextToken();
      }

      return new EtcdStoreStatsResponse(
          compareAndSwapFail,
          compareAndSwapSuccess,
          createFail,
          createSuccess,
          deleteFail,
          deleteSuccess,
          expireCount,
          getsFail,
          getsSuccess,
          setsFail,
          setsSuccess,
          updateFail,
          updateSuccess,
          watchers);
    }

    return null;
  }
}