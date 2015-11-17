package mousio.etcd4j.responses;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;
import java.nio.charset.Charset;

public class EtcdStringResponseDecoder implements EtcdResponseDecoder<String> {
  public static final EtcdStringResponseDecoder INSTANCE = new EtcdStringResponseDecoder();

  @Override
  public String decode(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
    return content.toString(Charset.defaultCharset());
  }
}
