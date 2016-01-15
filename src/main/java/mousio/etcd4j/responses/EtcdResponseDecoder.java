package mousio.etcd4j.responses;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;


public interface EtcdResponseDecoder<T> {
  /**
   * @param headers to decode with
   * @param content to decode
   * @return Object of type to which it is decoded to.
   * @throws IOException if content is faulty
   * @throws EtcdException If etcd returns an exception from the server
   */
  T decode(HttpHeaders headers, ByteBuf content)
      throws EtcdException, IOException;
}
