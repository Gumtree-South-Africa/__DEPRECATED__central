package com.ecg.de.ebayk.messagecenter.capi;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Create HTTP clients.
 */
public final class HttpClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientBuilder.class);

    private HttpClientBuilder() {
    }

    static HttpClient buildHttpClient(Configuration.HttpClient configuration) {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(configuration.maxConnections);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(configuration.maxConnections);

        final DefaultHttpRequestRetryHandler retryHandler = configuration.retryCount > 0 ? new ZealousHttpRequestRetryHandler(configuration.retryCount) : null;

        final RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(configuration.socketTimeout)
                .setConnectTimeout(configuration.connectionTimeout)
                .setConnectionRequestTimeout(configuration.connectionManagerTimeout)
                .build();

        return HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager)
                .setRetryHandler(retryHandler)
                .setDefaultRequestConfig(config)
                .build();
    }

    private static class ZealousHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
        ZealousHttpRequestRetryHandler(int retryCount) {
            super(retryCount, true, Collections.<Class<? extends IOException>>emptySet());
        }

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            if (super.retryRequest(exception, executionCount, context)) {
                LOG.info("Encountered {}; retrying...", exception.getClass().getSimpleName());
                return true;
            }

            return false;
        }
    }
}
