package mousio.etcd4j.responses;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static mousio.etcd4j.responses.EtcdKeysResponseDecoder.convertDate;

/**
 * Etcd Keys Response
 */
public final class EtcdKeysResponse {

  public final EtcdKeyAction action;
  public final EtcdNode node;
  public final EtcdNode prevNode;

  public final String etcdClusterId;
  public final Long etcdIndex;
  public final Long raftIndex;
  public final Long raftTerm;

  /**
   * Protected constructor
   *
   * @param action
   * @param node
   * @param prevNode
   * @param etcdClusterId
   * @param etcdIndex
   * @param raftIndex
   * @param raftTerm
   */
  EtcdKeysResponse(
      final String action,
      final EtcdNode node,
      final EtcdNode prevNode,
      final String etcdClusterId,
      final Long etcdIndex,
      final Long raftIndex,
      final Long raftTerm) {
    this.action = EtcdKeyAction.valueOf(action);
    this.node = node;
    this.prevNode = prevNode;

    this.etcdClusterId = etcdClusterId;
    this.etcdIndex = etcdIndex;
    this.raftIndex = raftIndex;
    this.raftTerm = raftTerm;
  }

  /**
   * An Etcd node
   */
  public static final class EtcdNode {
    public final String key;
    public final boolean dir;
    public final Long createdIndex;
    public final Long modifiedIndex;
    public final String value;
    public final Date expiration;
    public final Long ttl;
    public final List<EtcdNode> nodes;

    /**
     * Etcd Node
     *
     * @param dir
     * @param key
     * @param value
     * @param createdIndex
     * @param modifiedIndex
     * @param expiration
     * @param ttl
     * @param nodes
     */
    EtcdNode(
        final Boolean dir,
        final String key,
        final String value,
        final long createdIndex,
        final long modifiedIndex,
        final String expiration,
        final long ttl,
        final List<EtcdNode> nodes) {

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
          ? convertDate(expiration)
          : null;
    }
  }
}