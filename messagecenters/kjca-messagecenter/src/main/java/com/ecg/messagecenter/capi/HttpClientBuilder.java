package com.ecg.messagecenter.capi;

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

public final class HttpClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientBuilder.class);

    private HttpClientBuilder() {
    }

    static HttpClient buildHttpClient(HttpClientConfig httpClientConfig) {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(httpClientConfig.getMaxConnections());
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(httpClientConfig.getMaxConnections());

        final DefaultHttpRequestRetryHandler retryHandler = httpClientConfig.getRetryCount() > 0
                ? new ZealousHttpRequestRetryHandler(httpClientConfig.getRetryCount()) : null;

        final RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(httpClientConfig.getSocketTimeout())
                .setConnectTimeout(httpClientConfig.getConnectionTimeout())
                .setConnectionRequestTimeout(httpClientConfig.getConnectionManagerTimeout())
                .build();

        return HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager)
                .setRetryHandler(retryHandler)
                .setDefaultRequestConfig(config)
                .build();
    }

    private static class ZealousHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
        ZealousHttpRequestRetryHandler(int retryCount) {
            super(retryCount, true, Collections.emptySet());
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
