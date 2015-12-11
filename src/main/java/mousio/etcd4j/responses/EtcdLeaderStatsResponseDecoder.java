package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 * @author John Eke
 *
 * Parses Etcd Leader Stats Response
 */
public class EtcdLeaderStatsResponseDecoder extends AbstractJsonResponseDecoder<EtcdLeaderStatsResponse> {
    public static final EtcdLeaderStatsResponseDecoder INSTANCE = new EtcdLeaderStatsResponseDecoder();

    public static final String LEADER = "leader";
    public static final String FOLLOWERS = "followers";

    public static final String FOLLOWERINFO_LATENCY = "latency";
    public static final String FOLLOWERINFO_COUNTS = "counts";

    public static final String LATENCYINFO_CURRENT = "current";
    public static final String LATENCYINFO_AVERAGE = "average";
    public static final String LATENCYINFO_STANDARD_DEVIATION = "standardDeviation";
    public static final String LATENCYINFO_MIN = "minimum";
    public static final String LATENCYINFO_MAX = "maximum";

    public static final String COUNTSINFO_FAIL = "fail";
    public static final String COUNTSINFO_SUCCESS = "success";

    /**
     * Parses the Json content of the Etcd Leader StatsResponse
     *
     * @param headers
     * @param parser Json parser
     * @return EtcdLeaderStatsResponse if found in response
     * @throws mousio.etcd4j.responses.EtcdException if exception was found in response
     * @throws java.io.IOException                   if Json parsing or parser creation fails
     */
    @Override
    protected EtcdLeaderStatsResponse decodeJson(HttpHeaders headers, JsonParser parser) throws EtcdException, IOException {
        String leader = null;
        Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers = null;

        if (parser.nextToken() == JsonToken.START_OBJECT) {

            JsonToken token = parser.nextToken();
            while (token != JsonToken.END_OBJECT && token != null) {

                switch (parser.getCurrentName()) {
                    case LEADER:
                        leader = parser.nextTextValue();
                        break;
                    case FOLLOWERS:
                        followers = parseFollowers(parser, token);
                        break;
                }

                token = parser.nextToken();
            }

            return new EtcdLeaderStatsResponse(leader, followers);
        }

        return null;
    }

    private static Map<String, EtcdLeaderStatsResponse.FollowerInfo> parseFollowers(JsonParser parser, JsonToken token) throws EtcdException, IOException {
        Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers = new HashMap<>();

        if (parser.nextToken() == JsonToken.START_OBJECT) {
            while (token != JsonToken.END_OBJECT && token != null) {
                String name = parser.getCurrentName();
                switch(name) {
                    case FOLLOWERS:
                        break;
                    default:
                        followers.put(parser.getCurrentName(), parseFollowerInfo(parser, token));
                        break;
                }

                token = parser.nextToken();
            }
        }

        return followers;
    }

    private static EtcdLeaderStatsResponse.FollowerInfo parseFollowerInfo(JsonParser parser, JsonToken token) throws EtcdException, IOException {
        EtcdLeaderStatsResponse.LatencyInfo latency = null;
        EtcdLeaderStatsResponse.CountsInfo counts = null;

        if (parser.nextToken() == JsonToken.START_OBJECT) {
            while (token != JsonToken.END_OBJECT && token != null) {
                switch (parser.getCurrentName()) {
                    case FOLLOWERINFO_LATENCY:
                        latency = parseLatencyInfo(parser, token);
                        break;
                    case FOLLOWERINFO_COUNTS:
                        counts = parseCountsInfo(parser, token);
                        break;
                }

                token = parser.nextToken();
            }
            return new EtcdLeaderStatsResponse.FollowerInfo(latency, counts);
        }

        return null;
    }

    private static EtcdLeaderStatsResponse.LatencyInfo parseLatencyInfo(JsonParser parser, JsonToken token) throws EtcdException, IOException {
        if (parser.nextToken() == JsonToken.START_OBJECT) {

            double current = 0.0;
            double average = 0.0;
            double standardDeviation = 0.0;
            double minimum = 0.0;
            double maximum = 0.0;

            while (token != JsonToken.END_OBJECT && token != null) {
                if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                    switch (parser.getCurrentName()) {
                        case LATENCYINFO_CURRENT:
                            current = Double.parseDouble(parser.getValueAsString());
                            break;
                        case LATENCYINFO_AVERAGE:
                            average = Double.parseDouble(parser.getValueAsString());
                            break;
                        case LATENCYINFO_STANDARD_DEVIATION:
                            standardDeviation = Double.parseDouble(parser.getValueAsString());
                            break;
                        case LATENCYINFO_MIN:
                            minimum = Double.parseDouble(parser.getValueAsString());
                            break;
                        case LATENCYINFO_MAX:
                            maximum = Double.parseDouble(parser.getValueAsString());
                            break;
                    }
                }

                token = parser.nextToken();
            }

            return new EtcdLeaderStatsResponse.LatencyInfo(current, average, standardDeviation, minimum, maximum);
        }

        return null;
    }

    private static EtcdLeaderStatsResponse.CountsInfo parseCountsInfo(JsonParser parser, JsonToken token) throws EtcdException, IOException {
        if (parser.nextToken() == JsonToken.START_OBJECT) {

            long fail = 0;
            long success = 0;

            while (token != JsonToken.END_OBJECT && token != null) {
                if (token == JsonToken.VALUE_NUMBER_INT) {
                    switch (parser.getCurrentName()) {
                        case COUNTSINFO_FAIL:
                            fail = parser.getLongValue();
                            break;
                        case COUNTSINFO_SUCCESS:
                            success = parser.getLongValue();
                            break;
                    }
                }

                token = parser.nextToken();
            }

            return new EtcdLeaderStatsResponse.CountsInfo(fail, success);
        }

        return null;
    }
}
