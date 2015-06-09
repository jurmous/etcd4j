package mousio.etcd4j.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.handler.codec.http.HttpHeaders;

import javax.xml.bind.DatatypeConverter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static mousio.etcd4j.EtcdUtil.getHeaderPropertyAsLong;

/**
 * Etcd Keys Response
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public final class EtcdKeysResponse {

    private final EtcdKeyAction action;
    private final EtcdNode node;
    private final EtcdNode prevNode;

    private String etcdClusterId;
    private Long etcdIndex;
    private Long raftIndex;
    private Long raftTerm;

    public EtcdKeysResponse(
        @JsonProperty("action") final String action,
        @JsonProperty("node") final EtcdNode node,
        @JsonProperty("prevNode") final EtcdNode prevNode) {
        this.action = EtcdKeyAction.fromString(action);
        this.node = node;
        this.prevNode = prevNode;

        this.etcdClusterId = null;
        this.etcdIndex = null;
        this.raftIndex = null;
        this.raftTerm = null;
    }

    public EtcdKeyAction action() {
        return action;
    }

    public EtcdNode node() {
        return node;
    }

    public EtcdNode prevNode() {
        return prevNode;
    }

    public String etcdClusterId() {
        return etcdClusterId;
    }

    public Long etcdIndex() {
        return etcdIndex;
    }

    public Long raftIndex() {
        return raftIndex;
    }

    public Long raftTerm() {
        return raftTerm;
    }

    EtcdKeysResponse loadHeaders(HttpHeaders headers) {
        if(headers != null) {
            this.etcdClusterId = headers.get("X-Etcd-Cluster-Id");
            this.etcdIndex = getHeaderPropertyAsLong(headers, "X-Etcd-Index");
            this.raftIndex = getHeaderPropertyAsLong(headers, "X-Raft-Index");
            this.raftTerm = getHeaderPropertyAsLong(headers, "X-Raft-Term");
        }

        return this;
    }

    /**
    * An Etcd node
    */
    @JsonIgnoreProperties( ignoreUnknown = true )
    public static final class EtcdNode {
        private final String key;
        private final boolean dir;
        private final Long createdIndex;
        private final Long modifiedIndex;
        private final String value;
        private final Date expiration;
        private final Long ttl;
        private final List<EtcdNode> nodes;

        public EtcdNode(
            @JsonProperty("dir") final Boolean dir,
            @JsonProperty("key") final String key,
            @JsonProperty("value") final String value,
            @JsonProperty("createdIndex") final long createdIndex,
            @JsonProperty("modifiedIndex") final long modifiedIndex,
            @JsonProperty("expiration") final String expiration,
            @JsonProperty("ttl") final long ttl,
            @JsonProperty("nodes") final List<EtcdNode> nodes) {

            this.dir = dir != null ? dir : false;
            this.key = key;
            this.value = value;
            this.createdIndex = createdIndex;
            this.modifiedIndex = modifiedIndex;
            this.ttl = ttl;
            this.nodes = nodes != null
                ? Collections.unmodifiableList(nodes)
                : Collections.unmodifiableList(Collections.<EtcdNode>emptyList());
            this.expiration = expiration != null
                ? DatatypeConverter.parseDateTime(expiration).getTime()
                : null;
        }

        public boolean dir() {
            return dir;
        }

        public String key() {
            return key;
        }

        public long createdIndex() {
            return createdIndex;
        }

        public long modifiedIndex() {
            return modifiedIndex;
        }

        public long ttl() {
            return ttl;
        }

        public Date expiration() {
            return expiration;
        }

        public String value() {
            return value;
        }

        public List<EtcdNode> nodes() {
            return nodes;
        }
    }
}