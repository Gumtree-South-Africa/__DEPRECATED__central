package com.ecg.messagecenter.pushmessage.send.client;

import ca.kijiji.discovery.ServiceEndpoint;
import ca.kijiji.tracing.TraceLogFilter;
import ca.kijiji.tracing.TraceThreadLocal;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Hystrix-based container for SEND commands
 */
abstract class FailureAwareCommand<T> extends HystrixCommand<T> {
    private static final Logger LOG = LoggerFactory.getLogger(FailureAwareCommand.class);
    private final HttpClient httpClient;
    private final List<ServiceEndpoint> serviceEndpoints;
    private final String traceNumber;
    protected HttpRequestBase request;
    private SendException failure;

    public FailureAwareCommand(final HttpClient httpClient,
                               final List<ServiceEndpoint> serviceEndpoints) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("SEND"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("SEND")));

        this.httpClient = httpClient;
        this.serviceEndpoints = serviceEndpoints;
        this.traceNumber = TraceThreadLocal.get();
    }

    @Override
    protected T run() throws Exception {
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
                message.append("\n\n" + IOUtils.toString(responseContent));
            }
            throw new SendException(SendException.Cause.HTTP, message.toString(), null);
        } catch (Exception e) {
            failure = wrapException(e);
            logException(failure);
            throw failure;
        } finally {
            HttpClientUtils.closeQuietly(response);
            if (responseContent != null) {
                responseContent.close();
            }
        }
    }

    private HttpResponse getHttpResponse() throws IOException {
        for (ServiceEndpoint endpoint : serviceEndpoints) {
            try {
                final HttpHost host = new HttpHost(endpoint.address(), endpoint.port());
                final HttpResponse response = httpClient.execute(host, this.request);
                if (response.getStatusLine().getStatusCode() < 500) {
                    return response;
                }
            } catch (IOException e) {
                LOG.warn("Unable to process {} due to IOException. This is retryable.", request.getMethod(), e);
            }
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
