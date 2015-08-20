package mousio.etcd4j.responses;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.netty.handler.codec.http.HttpHeaders;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Date;

/**
 * Parses the JSON response for storage stats responses
 */
public class EtcdSelfStatsResponseDecoder extends AbstractJsonResponseDecoder<EtcdSelfStatsResponse> {
  public static final EtcdSelfStatsResponseDecoder INSTANCE = new EtcdSelfStatsResponseDecoder();

  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String RECVAPPENDREQUESTCNT = "recvAppendRequestCnt";
  public static final String RECVBANDWIDTHRATE = "recvBandwidthRate";
  public static final String RECVPKGRATE = "recvPkgRate";
  public static final String SENDAPPENDREQUESTCNT = "sendAppendRequestCnt";
  public static final String STARTTIME = "startTime";
  public static final String STATE = "state";
  public static final String LEADERINFO = "leaderInfo";

  public static final String LEADERINFO_LEADER = "leader";
  public static final String LEADERINFO_STARTTIME = "startTime";
  public static final String LEADERINFO_UPTIME = "uptime";

  /**
   * Parses the Json content of the Etcd Response
   *
   * @param headers
   * @param parser Json parser
   * @return EtcdSelfStatsResponse if found in response
   * @throws mousio.etcd4j.responses.EtcdException if exception was found in response
   * @throws java.io.IOException                   if Json parsing or parser creation fails
   */
  @Override
  protected EtcdSelfStatsResponse decodeJson(HttpHeaders headers, JsonParser parser) throws EtcdException, IOException {

    String id = null;
    String name = null;
    long recvAppendRequestCnt = 0;
    double recvBandwidthRate = 0;
    double recvPkgRate = 0;
    long sendAppendRequestCnt = 0;
    Date startTime = null;
    String state = null;
    EtcdSelfStatsResponse.LeaderInfo leaderInfo = null;

    if (parser.nextToken() == JsonToken.START_OBJECT) {

      JsonToken token = parser.nextToken();
      while (token != JsonToken.END_OBJECT && token != null) {

        switch (parser.getCurrentName()) {
          case ID:
            id = parser.nextTextValue();
            break;
          case NAME:
            name = parser.nextTextValue();
            break;
          case RECVAPPENDREQUESTCNT:
            recvAppendRequestCnt = parser.nextLongValue(0);
            break;
          case RECVBANDWIDTHRATE:
            recvBandwidthRate = Double.parseDouble(parser.nextTextValue());
            break;
          case RECVPKGRATE:
            recvBandwidthRate = Double.parseDouble(parser.nextTextValue());
            break;
          case SENDAPPENDREQUESTCNT:
            sendAppendRequestCnt = parser.nextLongValue(0);
            break;
          case STARTTIME:
            startTime = DatatypeConverter.parseDateTime(parser.nextTextValue()).getTime();
            break;
          case STATE:
            state = parser.nextTextValue();
            break;
          case LEADERINFO:
            leaderInfo = parseLeaderInfo(parser, token);
            break;
        }

        token = parser.nextToken();
      }

      return new EtcdSelfStatsResponse(
          id,
          name,
          recvAppendRequestCnt,
          recvBandwidthRate,
          recvPkgRate,
          sendAppendRequestCnt,
          startTime,
          state,
          leaderInfo);
    }

    return null;
  }

  private static EtcdSelfStatsResponse.LeaderInfo parseLeaderInfo(JsonParser parser, JsonToken token) throws EtcdException, IOException {
    if (parser.nextToken() == JsonToken.START_OBJECT) {

      String leader = null;
      Date startTime = null;
      String uptime = null;

      while (token != JsonToken.END_OBJECT && token != null) {

        switch (parser.getCurrentName()) {
          case LEADERINFO_LEADER:
            leader = parser.nextTextValue();
            break;
          case LEADERINFO_STARTTIME:
            startTime = DatatypeConverter.parseDateTime(parser.nextTextValue()).getTime();
            break;
          case LEADERINFO_UPTIME:
            uptime = parser.nextTextValue();
            break;
        }

        token = parser.nextToken();
      }

      return new EtcdSelfStatsResponse.LeaderInfo(leader, startTime, uptime);
    }

    return null;
  }
}