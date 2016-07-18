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
package mousio.etcd4j.responses;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Luca Burgazzoli
 */
public class EtcdResponseDecoders {
  protected static final CharSequence X_ETCD_CLUSTER_ID = "X-Etcd-Cluster-Id";
  protected static final CharSequence X_ETCD_INDEX = "X-Etcd-Index";
  protected static final CharSequence X_RAFT_INDEX = "X-Raft-Index";
  protected static final CharSequence X_RAFT_TERM = "X-Raft-Term";

  public static final EtcdResponseDecoder<String> STRING_DECODER = new StringDecoder();

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerModules(new AfterburnerModule());


  public static class JsonDecoder<T> implements EtcdResponseDecoder<T> {

    private final Class<T> type;

    public JsonDecoder(Class<T> type) {
      this.type = type;
    }

    @Override
    public T decode(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
      final DataInput di = new ByteBufInputStream(content);
      final T value = MAPPER.readValue(di, this.type);
      if(headers != null && EtcdHeaderAwareResponse.class.isAssignableFrom(this.type)) {
        ((EtcdHeaderAwareResponse) value).loadHeaders(headers);
      }

      return value;
    }
  }

  public static class StringDecoder implements EtcdResponseDecoder<String> {
    @Override
    public String decode(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
      return content.toString(Charset.defaultCharset());
    }
  }



  public abstract static class StringToObjectDecoder<T> implements EtcdResponseDecoder<T> {
    @Override
    public T decode(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
      return newInstance(content.toString(Charset.defaultCharset()));
    }

    protected abstract T newInstance(String message);
  }

  // ***************************************************************************
  //
  // ***************************************************************************


  public static <T> EtcdResponseDecoder<T> json(Class<T> type) {
    return new JsonDecoder<>(type);
  }

  public static EtcdResponseDecoder<String> string() {
    return new StringDecoder();
  }
}
