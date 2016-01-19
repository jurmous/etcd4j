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
 * Exception on etcd failures
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class EtcdException extends Exception {
  private static final long serialVersionUID = -3921194095313052325L;

  // The json
  public static final EtcdResponseDecoder<EtcdException> DECODER =
    EtcdResponseDecoders.json(EtcdException.class);

  public final String etcdCause;
  public final int errorCode;
  public final Long index;
  public final String etcdMessage;

  /**
   * Constructor
   *
   * @param errorCode the etcd error code
   * @param cause     the exception cause
   * @param message   the exception message
   * @param index     the data index
   */
  protected EtcdException(
    @JsonProperty("errorCode") Integer errorCode,
    @JsonProperty("cause") String cause,
    @JsonProperty("message") String message,
    @JsonProperty("index") Long index) {
    this.errorCode = errorCode;
    this.etcdCause = cause;
    this.etcdMessage = message;
    this.index = index;
  }

  @Override
  public String getMessage() {
    return String.format("[%s]: %s%s%s",
        errorCode,
        etcdMessage,
        ((etcdCause != null) ? ", cause: " + etcdCause : ""),
        ((index != null) ? ", at index: " + index : "")
    );
  }

  @Override
  public String toString() {
    return getMessage();
  }
}