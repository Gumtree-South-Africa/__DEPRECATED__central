package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.*;
import ca.kijiji.tracing.TraceLogFilter;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.netflix.hystrix.*;
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

    private static final double FUDGE_FACTOR = 1.25;
    private static final int READ_TIMEOUT = 100;
    private static final int CONNECT_TIMEOUT = 25;
    private static final int RETRY_BUFFER_MULTIPLIER = 3; // want to allow for some read timeouts and retries
    private static final int EXECUTION_TIMEOUT = ((int) ((READ_TIMEOUT + CONNECT_TIMEOUT) * FUDGE_FACTOR)) * RETRY_BUFFER_MULTIPLIER;
    private static final int THREADS = 5;
    private static final HystrixCommandProperties.Setter COMMAND_DEFAULTS = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(EXECUTION_TIMEOUT)
            .withFallbackEnabled(false);
    private static final HystrixThreadPoolProperties.Setter POOL_DEFAULTS = HystrixThreadPoolProperties.Setter()
            .withCoreSize(THREADS);

    private static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("UserBehaviourGroup");
    private static final HystrixThreadPoolKey POOL_KEY = HystrixThreadPoolKey.Factory.asKey("UserBehaviourPool");

    static final String SERVICE_NAME = "user-behaviour-service";
    static final Protocol HTTP = new Protocol("http");
    static final String ENDPOINT = "/responsiveness";

    private final ServiceDirectory serviceDirectory;
    private final CloseableHttpClient httpClient;
    private final String traceHeader;
    private final long uid;
    private final int timeToRespondSeconds;

    private final Counter failedRequestCounter;
    private final Counter errorRequestCounter;
    private final Counter discoveryFailedCounter;

    public SendResponsivenessToServiceCommand(
            ServiceDirectory serviceDirectory,
            CloseableHttpClient httpClient,
            String traceHeader,
            long uid,
            int timeToRespondSeconds,
            boolean testMode
    ) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolKey(POOL_KEY)
                .andCommandPropertiesDefaults(
                        COMMAND_DEFAULTS.withCircuitBreakerEnabled(!testMode).withExecutionTimeoutEnabled(!testMode)
                )
                .andThreadPoolPropertiesDefaults(POOL_DEFAULTS)
        );

        this.serviceDirectory = serviceDirectory;
        this.httpClient = httpClient;
        this.traceHeader = traceHeader;
        this.uid = uid;
        this.timeToRespondSeconds = timeToRespondSeconds;

        this.failedRequestCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.failed");
        this.errorRequestCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.error");
        this.discoveryFailedCounter = TimingReports.newCounter("user-behaviour.responsiveness.discovery.failed");
    }

    @Override
    protected Object run() throws Exception {
        HttpPost httpPost = new HttpPost(ENDPOINT);
        httpPost.addHeader(TraceLogFilter.TRACE_HEADER, traceHeader);
        httpPost.setEntity(new StringEntity(
                "{\"uid\":" + uid + ",\"ttr_s\":" + timeToRespondSeconds + "}",
                ContentType.APPLICATION_JSON
        ));

        List<ServiceEndpoint> serviceEndpoints = getServiceEndpoints();

        if (doPost(httpPost, serviceEndpoints)) {
            // success
            return null;
        }

        throw new RuntimeException("No success from any endpoints");
    }

    private List<ServiceEndpoint> getServiceEndpoints() throws DiscoveryFailedException {
        LookupRequest lookupRequest = new LookupRequest(SERVICE_NAME, HTTP);
        SelectAll selectionStrategy = new SelectAll();

        // Discovery lookups are not 100% successful, so retry once before giving up
        try {
            return serviceDirectory.lookup(selectionStrategy, lookupRequest).all();
        } catch (DiscoveryFailedException e) {
            LOG.info("First discovery attempt failed: {}. Retrying once.", e.toString());
        }

        // second (and last) try
        try {
            return serviceDirectory.lookup(selectionStrategy, lookupRequest).all();
        } catch (DiscoveryFailedException e) {
            discoveryFailedCounter.inc();
            LOG.warn("Second (final) discovery attempt failed");
            throw e;
        }
    }

    private boolean doPost(HttpPost httpPost, List<ServiceEndpoint> serviceEndpoints) {
        for (ServiceEndpoint endpoint : serviceEndpoints) {
            HttpHost httpHost = new HttpHost(endpoint.address(), endpoint.port());
            try (CloseableHttpResponse response = httpClient.execute(httpHost, httpPost)) {
                EntityUtils.consumeQuietly(response.getEntity());
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                    return true;
                } else {
                    failedRequestCounter.inc();
                    LOG.info("Received a non-204 response from {}: Status Line: {}. Trying next endpoint.", SERVICE_NAME, response.getStatusLine().toString());
                }
            } catch (Exception e) {
                errorRequestCounter.inc();
                LOG.info("Exception while calling {}. Trying next endpoint.", SERVICE_NAME, e);
            }
        }

        LOG.error("No endpoints in {} were able to satisfy the request", serviceEndpoints);
        return false;
    }
}
