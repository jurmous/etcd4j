package mousio.etcd4j.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Etcd Health response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EtcdHealthResponse implements EtcdResponse {

	// The json
	public static final EtcdResponseDecoder<EtcdHealthResponse> DECODER =
		EtcdResponseDecoders.json(EtcdHealthResponse.class);

	private final String health;

	EtcdHealthResponse(
		@JsonProperty("health") final String health) {
		this.health = health;
	}

	public String getHealth() {
		return health;
	}
}
