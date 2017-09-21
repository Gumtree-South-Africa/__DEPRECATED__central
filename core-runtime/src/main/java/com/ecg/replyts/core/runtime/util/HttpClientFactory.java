package com.ecg.replyts.core.runtime.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

public class HttpClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);

    public static CloseableHttpClient createCloseableHttpClient(int connectionTimeout,
                                                                int connectionRequestTimeout,
                                                                int socketTimeout,
                                                                int maxConnectionsPerHost,
                                                                int maxTotalConnections) {
        return HttpClientBuilder.create()
                .setMaxConnPerRoute(maxConnectionsPerHost)
                .setMaxConnTotal(maxTotalConnections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(connectionRequestTimeout)
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(socketTimeout)
                        .build())
                .build();
    }

    public static void closeWithLogging(@Nonnull CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            LOG.warn("Failed to close the http client due to an error: ", e.getMessage());
        }
    }
}