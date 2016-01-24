package mousio.etcd4j;

import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mousio.etcd4j.responses.EtcdVersionResponse;

public class EtcdClientRule implements TestRule {

  private static Logger logger = LoggerFactory.getLogger(EtcdClientRule.class);

  private long wait = 100;
  private TimeUnit waitUnit = TimeUnit.MILLISECONDS;
  private long maxWait = 1;
  private TimeUnit maxWaitUnit = TimeUnit.MINUTES;

  private EtcdClient client;

  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          client = new EtcdClient();
          EtcdVersionResponse response;

          long startMillis = System.currentTimeMillis();
          while( true ) {
            try {
              response = client.version();
              break;
            }
            catch( Throwable e ) {
              if( (System.currentTimeMillis() - startMillis) + waitUnit.toMillis(wait) > maxWaitUnit.toMillis(maxWait)) {
                throw new IllegalStateException("etcd server not available", e);
              }
              logger.info("etcd not ready, waiting {} {}", wait, waitUnit);
              waitUnit.sleep(wait);
            }
          }

          logger.info("etcd server {} cluster {}", response.server, response.cluster);

          statement.evaluate();

        }
        finally {
          try {
            client.close();
          }
          catch(Exception e) {
            e.printStackTrace(System.err);
          }
        }
      }

    };
  }

  public EtcdClient getClient() {
    return client;
  }

}
