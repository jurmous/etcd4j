package mousio.etcd4j.responses;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Etcd Keys Response
 */
public class EtcdKeysResponse {

  public final EtcdKeyAction action;
  public final EtcdNode node;
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
    public Integer createdIndex;
    public Integer modifiedIndex;
    public String value;
    public ZonedDateTime expiration;
    public Integer ttl;
    public List<EtcdNode> nodes;
  }

}