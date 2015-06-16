package mousio.etcd4j;

import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Date;

public class EtcdUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdUtil.class);

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

    public static Date convertDate(String date) throws IOException {
        return DatatypeConverter.parseDateTime(date).getTime();
    }
}
