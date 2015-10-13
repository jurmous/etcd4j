package mousio.etcd4j.responses;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;

/**
 * Etcd Self Stats response
 */
public class EtcdSelfStatsResponse {

  private final String id;
  private final String name;
  private final long recvAppendRequestCnt;
  private final double recvBandwidthRate;
  private final double recvPkgRate;
  private final long sendAppendRequestCnt;
  private final Date startTime;
  private final String state;
  private final LeaderInfo leaderInfo;

  EtcdSelfStatsResponse(
      String id,
      String name,
      long recvAppendRequestCnt,
      double recvBandwidthRate,
      double recvPkgRate,
      long sendAppendRequestCnt,
      Date startTime,
      String state,
      LeaderInfo leaderInfo) {
    this.id = id;
    this.name = name;
    this.recvAppendRequestCnt = recvAppendRequestCnt;
    this.recvBandwidthRate = recvBandwidthRate;
    this.recvPkgRate = recvPkgRate;
    this.sendAppendRequestCnt = sendAppendRequestCnt;
    this.state = state;
    this.leaderInfo = leaderInfo;
    this.startTime = startTime;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getRecvAppendRequestCnt() {
    return recvAppendRequestCnt;
  }

  public double getRecvBandwidthRate() {
    return recvBandwidthRate;
  }

  public double getRecvPkgRate() {
    return recvPkgRate;
  }

  public long getSendAppendRequestCnt() {
    return sendAppendRequestCnt;
  }

  public Date getStartTime() {
    return startTime;
  }

  public String getState() {
    return state;
  }

  public LeaderInfo getLeaderInfo() {
    return leaderInfo;
  }

  public static class LeaderInfo {

    private final String leader;
    private final Date startTime;
    private final String uptime;

    LeaderInfo(
        String leader,
        Date startTime,
        String uptime) {

      this.leader = leader;
      this.uptime = uptime;
      this.startTime = startTime;
    }

    public String getLeader() {
      return leader;
    }

    public Date getStartTime() {
      return startTime;
    }

    public String getUptime() {
      return uptime;
    }
  }
}
