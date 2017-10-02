package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.ServiceEndpoint;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.tracing.TraceLogFilter;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes the request to user behaviour service with circuit breaker semantics.
 */
public class SendResponsivenessToServiceCommand extends HystrixCommand {

    private static final Logger LOG = LoggerFactory.getLogger(SendResponsivenessToServiceCommand.class);

    private static final String ENDPOINT = "/responsiveness";

    private final EndpointDiscoveryService endpointDiscoveryService;
    private final CloseableHttpClient httpClient;
    private final Counter requestFailedCounter;
    private final Counter requestErrorCounter;

    private ResponsivenessRecord responsivenessRecord;

    public SendResponsivenessToServiceCommand(EndpointDiscoveryService endpointDiscoveryService, CloseableHttpClient httpClient, Setter userBehaviourHystrixConfig) {
        super(userBehaviourHystrixConfig);

        this.endpointDiscoveryService = endpointDiscoveryService;
        this.httpClient = httpClient;
        this.requestFailedCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.failed");
        this.requestErrorCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.error");
    }

    public void setResponsivenessRecord(ResponsivenessRecord responsivenessRecord) {
        this.responsivenessRecord = responsivenessRecord;
    }

    @Override
    protected Void run() throws Exception {
        if (doPost(prepareRequest(), endpointDiscoveryService.discoverEndpoints())) {
            return null;
        }

        throw new RuntimeException("No success from any endpoints");
    }

    private HttpPost prepareRequest() {
        if (responsivenessRecord == null) {
            throw new IllegalArgumentException("Responsiveness record should be provided");
        }

        HttpPost httpPost = new HttpPost(ENDPOINT);
        httpPost.addHeader(TraceLogFilter.TRACE_HEADER, responsivenessRecord.getConversationId() + "/" + responsivenessRecord.getMessageId());
        httpPost.setEntity(new StringEntity(
                "{\"uid\":" + responsivenessRecord.getUserId() + ",\"ttr_s\":" + responsivenessRecord.getTimeToRespondInSeconds() + "}",
                ContentType.APPLICATION_JSON
        ));
        return httpPost;
    }

    private boolean doPost(HttpPost httpPost, List<ServiceEndpoint> serviceEndpoints) {
        for (ServiceEndpoint endpoint : serviceEndpoints) {
            HttpHost httpHost = new HttpHost(endpoint.address(), endpoint.port());
            try (CloseableHttpResponse response = httpClient.execute(httpHost, httpPost)) {
                EntityUtils.consumeQuietly(response.getEntity());
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    return true;
                } else {
                    requestFailedCounter.inc();
                    LOG.info("Received a non-204 response from {}: Status Line: {}. Trying next endpoint.",
                            endpoint.address(), response.getStatusLine().toString());
                }
            } catch (Exception e) {
                requestErrorCounter.inc();
                LOG.info("Exception while calling " + endpoint.address() + ". Trying next endpoint.", e);
            }
        }

        LOG.error("No endpoints in {} were able to satisfy the request", serviceEndpoints);
        return false;
    }
}
