package com.ecg.messagecenter.kjca.pushmessage.send.client;

import com.ecg.comaas.kjca.coremod.shared.TraceLogFilter;
import com.ecg.comaas.kjca.coremod.shared.TraceThreadLocal;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.google.common.io.Closeables;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClosedInputStream;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.InputStream;

/**
 * Hystrix-based container for SEND commands
 */
abstract class FailureAwareCommand<T> extends HystrixCommand<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FailureAwareCommand.class);
    private final HttpClient httpClient;
    private final HttpHost httpHost;
    private final String traceNumber;
    protected HttpRequestBase request;
    private SendException failure;
    private String correlationId;

    public FailureAwareCommand(final HttpClient httpClient, HttpHost httpHost, int hystrixTimeout) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("SEND"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("SEND"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(hystrixTimeout))
        );

        this.httpClient = httpClient;
        this.httpHost = httpHost;
        this.traceNumber = TraceThreadLocal.get();
        this.correlationId = MDC.get(MDCConstants.CORRELATION_ID);
    }

    @Override
    protected T run() throws Exception {
        setMDCFields();

        HttpResponse response = null;

        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        request.setHeader(TraceLogFilter.TRACE_HEADER, traceNumber);

        InputStream responseContent = null;
        try {
            response = getHttpResponse();
            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();

            if (response.getEntity() != null) {
                responseContent = response.getEntity().getContent();
            } else {
                responseContent = new ClosedInputStream();
            }

            if (statusCode < 300) {
                return successCallback(responseContent);
            }

            if (statusCode == HttpStatus.SC_CONFLICT) {
                failure = new SendException(SendException.Cause.CONFLICT, "", null);
                return null;
            }

            final StringBuilder message = new StringBuilder("HTTP " + statusCode + " " + statusLine.getReasonPhrase());
            if (responseContent.available() != 0) {
                message.append("\n\n").append(IOUtils.toString(responseContent, "UTF-8"));
            }
            throw new SendException(SendException.Cause.HTTP, message.toString(), null);
        } catch (Exception e) {
            failure = wrapException(e);
            logException(failure);
            throw failure;
        } finally {
            HttpClientUtils.closeQuietly(response);
            Closeables.closeQuietly(responseContent);
        }
    }

    private void setMDCFields() {
        MDC.clear();
        MDC.put(MDCConstants.CORRELATION_ID, correlationId);
        MDC.put(MDCConstants.TASK_NAME, this.getClass().getSimpleName());
    }

    private HttpResponse getHttpResponse() throws IOException {
        try {
            final HttpResponse response = httpClient.execute(this.httpHost, this.request);
            if (response.getStatusLine().getStatusCode() < 500) {
                return response;
            }
            HttpClientUtils.closeQuietly(response);
        } catch (IOException e) {
            LOG.warn("Unable to process {} due to IOException. This is retryable.", request.getMethod(), e);
        }
        throw new SendException(SendException.Cause.HTTP, "No endpoints were able to successfully handle this " + request.getMethod(), null);
    }

    private SendException wrapException(Exception e) {
        if (e.getClass() != SendException.class) {
            return new SendException(e);
        }

        return (SendException) e;
    }

    abstract protected T successCallback(InputStream responseContent) throws IOException;

    @Override
    protected T getFallback() {
        return null;
    }

    private void logException(final SendException e) {
        LOG.warn(exceptionMessageTemplate(), e.getInternalCause().name(), e.getMessage());
    }

    abstract protected String exceptionMessageTemplate();

    public SendException getFailure() {
        return failure;
    }
}
