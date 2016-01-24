package mousio.etcd4j;

import org.junit.ClassRule;
import org.junit.Test;

public class EtcdClientRuleTest {
  @ClassRule public static EtcdClientRule classRule = new EtcdClientRule();
  @Test
  public void example() {
	  System.out.println(classRule.getClient().version().server);
  }
}
