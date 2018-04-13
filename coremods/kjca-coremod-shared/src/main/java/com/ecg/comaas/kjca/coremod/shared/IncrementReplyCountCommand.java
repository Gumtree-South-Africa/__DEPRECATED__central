package com.ecg.comaas.kjca.coremod.shared;

import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.netflix.hystrix.*;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;


class IncrementReplyCountCommand extends HystrixCommand<Void>{
    private static final Logger LOG = LoggerFactory.getLogger(IncrementReplyCountCommand.class);
    private static final int EXECUTION_TIMEOUT_MILLIS = 1025;
    private static final int THREAD_POOL_SIZE = 5;
    private static final HystrixCommandProperties.Setter COMMAND_DEFAULTS = HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(EXECUTION_TIMEOUT_MILLIS);
    private static final HystrixThreadPoolProperties.Setter POOL_DEFAULTS = HystrixThreadPoolProperties.Setter()
            .withCoreSize(THREAD_POOL_SIZE);

    private static final HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("IncrementReplyCountGroup");
    private static final HystrixThreadPoolKey POOL_KEY = HystrixThreadPoolKey.Factory.asKey("IncrementReplyCountPool");

    private final CloseableHttpClient httpClient;
    private final URI path;
    private final String authHeader;
    private final String correlationId;

    IncrementReplyCountCommand(CloseableHttpClient httpClient, URI path, String authHeader) {
        super(Setter
                .withGroupKey(GROUP_KEY)
                .andThreadPoolKey(POOL_KEY)
                .andCommandPropertiesDefaults(COMMAND_DEFAULTS)
                .andThreadPoolPropertiesDefaults(POOL_DEFAULTS)
        );
        this.httpClient = httpClient;
        this.path = path;
        this.authHeader = authHeader;
        this.correlationId = MDC.get(MDCConstants.CORRELATION_ID);
    }

    @Override
    protected Void run() throws Exception {
        setMDCFields();
        HttpPost request = new HttpPost(path);
        request.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
        try(CloseableHttpResponse response = httpClient.execute(request)){
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 300) {
                LOG.warn("Got unexpected [{}] from [{}].", response.getStatusLine(), path);
            }
        } catch (Exception e) {
            LOG.warn("Got exception from [{}]", path, e);
        }
        return null;
    }

    private void setMDCFields() {
        MDC.clear();
        MDC.put(MDCConstants.CORRELATION_ID, correlationId);
        MDC.put(MDCConstants.TASK_NAME, this.getClass().getSimpleName());
    }
}
