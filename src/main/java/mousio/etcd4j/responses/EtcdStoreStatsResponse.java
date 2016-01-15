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

/**
 * @author Luca Burgazzoli
 *
 * Etcd Store Stats response
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class EtcdStoreStatsResponse implements EtcdResponse {

  // The json
  public static final EtcdResponseDecoder<EtcdStoreStatsResponse> DECODER =
    EtcdResponseDecoders.json(EtcdStoreStatsResponse.class);

  private final long compareAndSwapFail;
  private final long compareAndSwapSuccess;
  private final long createFail;
  private final long createSuccess;
  private final long deleteFail;
  private final long deleteSuccess;
  private final long expireCount;
  private final long getsFail;
  private final long getsSuccess;
  private final long setsFail;
  private final long setsSuccess;
  private final long updateFail;
  private final long updateSuccess;
  private final long watchers;

  EtcdStoreStatsResponse(
      @JsonProperty("compareAndSwapFail") long compareAndSwapFail,
      @JsonProperty("compareAndSwapSuccess") long compareAndSwapSuccess,
      @JsonProperty("createFail") long createFail,
      @JsonProperty("createSuccess") long createSuccess,
      @JsonProperty("deleteFail") long deleteFail,
      @JsonProperty("deleteSuccess") long deleteSuccess,
      @JsonProperty("expireCount") long expireCount,
      @JsonProperty("getsFail") long getsFail,
      @JsonProperty("getsSuccess") long getsSuccess,
      @JsonProperty("setsFail") long setsFail,
      @JsonProperty("setsSuccess") long setsSuccess,
      @JsonProperty("updateFail") long updateFail,
      @JsonProperty("updateSuccess") long updateSuccess,
      @JsonProperty("watchers") long watchers) {

    this.compareAndSwapFail=compareAndSwapFail;
    this.compareAndSwapSuccess=compareAndSwapSuccess;
    this.createFail=createFail;
    this.createSuccess=createSuccess;
    this.deleteFail=deleteFail;
    this.deleteSuccess=deleteSuccess;
    this.expireCount=expireCount;
    this.getsFail=getsFail;
    this.getsSuccess=getsSuccess;
    this.setsFail=setsFail;
    this.setsSuccess=setsSuccess;
    this.updateFail=updateFail;
    this.updateSuccess=updateSuccess;
    this.watchers=watchers;
  }

  public long getCompareAndSwapFail() {
    return compareAndSwapFail;
  }

  public long getCompareAndSwapSuccess() {
    return compareAndSwapSuccess;
  }

  public long getCreateFail() {
    return createFail;
  }

  public long getCreateSuccess() {
    return createSuccess;
  }

  public long getDeleteFail() {
    return deleteFail;
  }

  public long getDeleteSuccess() {
    return deleteSuccess;
  }

  public long getExpireCount() {
    return expireCount;
  }

  public long getGetsFail() {
    return getsFail;
  }

  public long getsSuccess() {
    return getsSuccess;
  }

  public long setsFail() {
    return setsFail;
  }

  public long setsSuccess() {
    return setsSuccess;
  }

  public long updateFail() {
    return updateFail;
  }

  public long updateSuccess() {
    return updateSuccess;
  }

  public long watchers() {
    return watchers;
  }
}
