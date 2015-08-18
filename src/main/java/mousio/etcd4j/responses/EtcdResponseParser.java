package mousio.etcd4j.responses;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;


public interface EtcdResponseParser<T> {
  /**
   * @param headers
   * @param content
   * @return
   * @throws EtcdException
   * @throws IOException
   */
  public T parse(HttpHeaders headers, ByteBuf content)
      throws EtcdException, IOException;
}
