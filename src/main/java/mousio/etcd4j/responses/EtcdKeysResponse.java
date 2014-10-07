package mousio.etcd4j.responses;

import java.util.Date;
import java.util.List;

/**
 * Etcd Keys Response
 */
public class EtcdKeysResponse {

  public final EtcdKeyAction action;
  public final EtcdNode node;
  public Long etcdIndex;
  public EtcdNode prevNode;

  /**
   * Constructs a new EtcdResponse
   *
   * @param action to set EtcdResponse with
   * @param node   with the Response data
   */
  public EtcdKeysResponse(String action, EtcdNode node) {
    this.action = EtcdKeyAction.valueOf(action);
    this.node = node;
  }

  /**
   * An Etcd node
   */
  public static class EtcdNode {
    public String key;
    public boolean dir = false;
    public Long createdIndex;
    public Long modifiedIndex;
    public String value;
    public Date expiration;
    public Long ttl;
    public List<EtcdNode> nodes;
  }
}