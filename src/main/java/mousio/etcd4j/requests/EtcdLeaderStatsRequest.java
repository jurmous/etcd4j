package mousio.etcd4j.requests;

import io.netty.handler.codec.http.HttpMethod;
import mousio.client.retry.RetryPolicy;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdLeaderStatsResponse;
import mousio.etcd4j.responses.EtcdLeaderStatsResponseDecoder;
import mousio.etcd4j.transport.EtcdClientImpl;

import java.io.IOException;


/**
 * @author Jurriaan Mous
 * @author Luca Burgazzoli
 * @author John Eke
 *
 * An Etcd Leader Stats Request
 */
public class EtcdLeaderStatsRequest extends EtcdRequest<EtcdLeaderStatsResponse> {

    /**
     * Constructor
     *
     * @param clientImpl   the client to handle this request
     * @param retryHandler handles retries
     */
    public EtcdLeaderStatsRequest(EtcdClientImpl clientImpl, RetryPolicy retryHandler) {
        super(clientImpl, HttpMethod.GET, retryHandler, EtcdLeaderStatsResponseDecoder.INSTANCE);
    }

    @Override public EtcdResponsePromise<EtcdLeaderStatsResponse> send() throws IOException {
        return clientImpl.send(this);
    }

    @Override public EtcdLeaderStatsRequest setRetryPolicy(RetryPolicy retryPolicy) {
        super.setRetryPolicy(retryPolicy);
        return this;
    }

    @Override public String getUri() {
        return "/v2/stats/leader";
    }
}
