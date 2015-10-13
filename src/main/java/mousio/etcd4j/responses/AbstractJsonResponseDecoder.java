package mousio.etcd4j.responses;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

public abstract class AbstractJsonResponseDecoder<T> implements EtcdResponseDecoder<T> {
  private static final JsonFactory FACTORY = new JsonFactory();

  public T decode(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
    return decodeJson(headers, FACTORY.createParser(new ByteBufInputStream(content)));
  }

  protected abstract T decodeJson(HttpHeaders headers, JsonParser parser)
      throws EtcdException, IOException;
}
