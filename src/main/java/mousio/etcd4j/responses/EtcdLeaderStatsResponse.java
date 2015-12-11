package mousio.etcd4j.responses;

import java.util.List;
import java.util.Map;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 * @author John Eke
 *
 * An Etcd Leader Stats Response
 */
public class EtcdLeaderStatsResponse {
    private final String leader;
    private final Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers;

    public EtcdLeaderStatsResponse(String leader, Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers) {
        this.leader = leader;
        this.followers = followers;
    }

    public String getLeader() {
        return leader;
    }

    public Map<String, EtcdLeaderStatsResponse.FollowerInfo> getFollowers() {
        return followers;
    }

    public static class FollowerInfo {
        private final LatencyInfo latency;
        private final CountsInfo counts;

        public FollowerInfo(
                LatencyInfo latency,
                CountsInfo counts
        ) {
            this.latency = latency;
            this.counts = counts;
        }

        public LatencyInfo getLatency() {
            return latency;
        }

        public CountsInfo getCounts() {
            return counts;
        }
    }

    public static class LatencyInfo {
        private final double current;
        private final double average;
        private final double standardDeviation;
        private final double minimum;
        private final double maximum;

        public LatencyInfo(
                double current,
                double average,
                double standardDeviation,
                double minimum,
                double maximum
        ) {
            this.current = current;
            this.average = average;
            this.standardDeviation = standardDeviation;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public double getCurrent() {
            return current;
        }

        public double getAverage() {
            return average;
        }

        public double getStandardDeviation() {
            return standardDeviation;
        }

        public double getMinimum() {
            return minimum;
        }

        public double getMaximum() {
            return maximum;
        }
    }

    public static class CountsInfo {
        private final long fail;
        private final long success;

        public CountsInfo(long fail, long success) {
            this.fail = fail;
            this.success = success;
        }

        public long getFail() {
            return fail;
        }

        public long getSuccess() {
            return success;
        }
    }
}
