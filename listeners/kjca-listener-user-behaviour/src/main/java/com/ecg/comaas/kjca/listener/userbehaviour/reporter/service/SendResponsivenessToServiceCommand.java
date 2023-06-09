package com.ecg.comaas.kjca.listener.userbehaviour.reporter.service;

import com.ecg.comaas.kjca.coremod.shared.TraceLogFilter;
import com.codahale.metrics.Counter;
import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.exception.HttpRequestFailedException;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.exception.IncorrectHttpStatusCodeException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Executes the request to user behaviour service with circuit breaker semantics.
 */
public class SendResponsivenessToServiceCommand extends HystrixCommand {

    private static final Logger LOG = LoggerFactory.getLogger(SendResponsivenessToServiceCommand.class);

    private static final String ENDPOINT = "/responsiveness";

    private final CloseableHttpClient httpClient;
    private final HttpHost httpHost;
    private final Counter requestFailedCounter;
    private final Counter requestErrorCounter;
    private final String correlationId;

    private ResponsivenessRecord responsivenessRecord;

    public SendResponsivenessToServiceCommand(CloseableHttpClient httpClient, Setter userBehaviourHystrixConfig, HttpHost httpHost) {
        super(userBehaviourHystrixConfig);

        this.correlationId = MDC.get(MDCConstants.CORRELATION_ID);
        this.httpClient = httpClient;
        this.httpHost = httpHost;
        this.requestFailedCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.failed");
        this.requestErrorCounter = TimingReports.newCounter("user-behaviour.responsiveness.request.error");
    }

    public void setResponsivenessRecord(ResponsivenessRecord responsivenessRecord) {
        this.responsivenessRecord = responsivenessRecord;
    }

    @Override
    protected Void run() throws Exception {
        setMDCFields();
        doPost(prepareRequest());
        return null;
    }

    private void setMDCFields() {
        MDC.clear();
        MDC.put(MDCConstants.CORRELATION_ID, correlationId);
        MDC.put(MDCConstants.TASK_NAME, SendResponsivenessToServiceCommand.class.getSimpleName());
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

    private void doPost(HttpPost httpPost) {
        try (CloseableHttpResponse response = httpClient.execute(httpHost, httpPost)) {
            EntityUtils.consumeQuietly(response.getEntity());
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                requestFailedCounter.inc();
                LOG.warn("Received a non-204 response from {}: {}.", httpHost.getHostName(), toString());
                throw new IncorrectHttpStatusCodeException("Expected to obtain HTTP 204 from " + httpHost.getHostName()
                        + ", instead got " + statusLine.getStatusCode());
            }
        } catch (Exception e) {
            requestErrorCounter.inc();
            LOG.error("Exception while calling {}", httpHost.getHostName(), e);
            throw new HttpRequestFailedException("Exception while calling " + httpHost.getHostName(), e);
        }
    }
}
