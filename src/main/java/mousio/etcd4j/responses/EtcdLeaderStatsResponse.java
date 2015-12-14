/*
 * Copyright (c) 2015, Jurriaan Mous and contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mousio.etcd4j.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 * @author John Eke
 *
 * An Etcd Leader Stats Response
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class EtcdLeaderStatsResponse implements EtcdResponse {

  // The json
  public static final EtcdResponseDecoder<EtcdLeaderStatsResponse> DECODER =
    EtcdResponseDecoders.json(EtcdLeaderStatsResponse.class);

  private final String leader;
  private final Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers;

  public EtcdLeaderStatsResponse(
    @JsonProperty("leader") String leader,
    @JsonProperty("followers") Map<String, EtcdLeaderStatsResponse.FollowerInfo> followers) {
    this.leader = leader;
    this.followers = Collections.unmodifiableMap(followers);
  }

  public String getLeader() {
    return leader;
  }

  public Map<String, EtcdLeaderStatsResponse.FollowerInfo> getFollowers() {
    return followers;
  }

  @JsonIgnoreProperties( ignoreUnknown = true )
  public static class FollowerInfo {
    private final LatencyInfo latency;
    private final CountsInfo counts;

    public FollowerInfo(
      @JsonProperty("latency") LatencyInfo latency,
      @JsonProperty("counts") CountsInfo counts) {
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

  @JsonIgnoreProperties( ignoreUnknown = true )
  public static class LatencyInfo {
    private final double current;
    private final double average;
    private final double standardDeviation;
    private final double minimum;
    private final double maximum;

    public LatencyInfo(
      @JsonProperty("current") double current,
      @JsonProperty("average") double average,
      @JsonProperty("standardDeviation") double standardDeviation,
      @JsonProperty("minimum") double minimum,
      @JsonProperty("maximum") double maximum) {
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

  @JsonIgnoreProperties( ignoreUnknown = true )
  public static class CountsInfo {
    private final long fail;
    private final long success;

    public CountsInfo(
      @JsonProperty("fail") long fail,
      @JsonProperty("success") long success) {
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
