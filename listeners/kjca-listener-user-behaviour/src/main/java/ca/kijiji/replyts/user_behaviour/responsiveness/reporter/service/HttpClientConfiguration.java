package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfiguration {
    @Value("${user-behaviour.httpclient.maxConnections}")
    private int maxConnections;

    @Value("${user-behaviour.httpclient.socketTimeoutMs}")
    private int socketTimeoutMillis;

    @Value("${user-behaviour.httpclient.connectTimeoutMs}")
    private int connectTimeoutMillis;

    @Value("${user-behaviour.httpclient.poolRequestTimeoutMs}")
    private int poolRequestTimeoutMillis;

    @Value("${user-behaviour.httpclient.retryCount}")
    private int retryCount;

    @Bean
    CloseableHttpClient userBehaviourHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);

        final RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(socketTimeoutMillis)
                .setConnectTimeout(connectTimeoutMillis)
                .setConnectionRequestTimeout(poolRequestTimeoutMillis)
                .build();

        final DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(retryCount, false);

        return HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .setRetryHandler(retryHandler)
                .build();
    }
}
