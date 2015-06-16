package mousio.etcd4j.responses;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;

/**
 * Parses the JSON response for key responses
 */
public class EtcdKeysResponseParser {
    private static final ObjectMapper MAPPER =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Parses the Json content of the Etcd Response
     *
     * @param headers the http header
     * @param content to parse
     *
     * @return EtcdResponse if found in response
     * @throws mousio.etcd4j.responses.EtcdException if exception was found in response
     * @throws java.io.IOException                   if Json parsing or parser creation fails
     */
    public static EtcdKeysResponse parseResponse(HttpHeaders headers, ByteBuf content) throws EtcdException, IOException {
        return MAPPER.readValue(new ByteBufInputStream(content), EtcdKeysResponse.class).loadHeaders(headers);
    }

    /**
     * Parses an EtcdException
     *
     * @param headers the http header
     * @param content to parse
     *
     * @return EtcdException
     * @throws java.io.IOException IOException
     */
    public static EtcdException parseException(HttpHeaders headers, ByteBuf content) throws IOException {
        return MAPPER.readValue(new ByteBufInputStream(content), EtcdException.class);
    }
}