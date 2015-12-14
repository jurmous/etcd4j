package mousio.etcd4j.responses;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;


public interface EtcdResponseDecoder<T> {
  /**
   * @param headers
   * @param content
   * @return
   * @throws EtcdException
   * @throws IOException
   */
  T decode(HttpHeaders headers, ByteBuf content)
      throws EtcdException, IOException;
}
