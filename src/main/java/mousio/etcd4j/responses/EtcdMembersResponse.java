package mousio.etcd4j.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Etcd Members response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EtcdMembersResponse implements EtcdResponse {

	// The json
	public static final EtcdResponseDecoder<EtcdMembersResponse> DECODER =
		EtcdResponseDecoders.json(EtcdMembersResponse.class);

	private final List<MemberInfo> members;

	EtcdMembersResponse(
		@JsonProperty("members") List<MemberInfo> members) {
		this.members = Collections.unmodifiableList(members);
	}

	public List<MemberInfo> getMembers() {
		return members;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MemberInfo {

		private final String id;
		private final String name;
		private final List<String> peerURLs;
		private final List<String> clientURLs;

		MemberInfo(
			@JsonProperty("id") final String id,
			@JsonProperty("name") final String name,
			@JsonProperty("peerURLs") final List<String> peerURLs,
			@JsonProperty("clientURLs") final List<String> clientURLs) {

			this.id = id;
			this.name = name;
			this.peerURLs = Collections.unmodifiableList(peerURLs);
			this.clientURLs = Collections.unmodifiableList(clientURLs);
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<String> getPeerURLs() {
			return peerURLs;
		}

		public List<String> getClientURLs() {
			return clientURLs;
		}
	}
}
