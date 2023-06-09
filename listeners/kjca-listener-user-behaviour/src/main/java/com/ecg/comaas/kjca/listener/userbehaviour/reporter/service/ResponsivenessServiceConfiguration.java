package com.ecg.comaas.kjca.listener.userbehaviour.reporter.service;

import com.ecg.comaas.kjca.listener.userbehaviour.UserResponsivenessListener;
import com.ecg.comaas.kjca.listener.userbehaviour.hystrix.metrics.RTSHystrixCodaHaleMetricsPublisher;
import com.ecg.replyts.core.runtime.MetricsService;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.strategy.HystrixPlugins;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(UserResponsivenessListener.class)
public class ResponsivenessServiceConfiguration {

    @Value("${user-behaviour.httpclient.maxConnections:10}")
    private int maxConnections;

    @Value("${user-behaviour.httpclient.socketTimeoutMs:1000}")
    private int socketTimeoutMillis;

    @Value("${user-behaviour.httpclient.connectTimeoutMs:50}")
    private int connectTimeoutMillis;

    @Value("${user-behaviour.httpclient.poolRequestTimeoutMs:50}")
    private int poolRequestTimeoutMillis;

    @Value("${user-behaviour.httpclient.retryCount:1}")
    private int retryCount;

    @Bean
    public CloseableHttpClient userBehaviourHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(maxConnections);
        connectionManager.setMaxTotal(maxConnections);

        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(socketTimeoutMillis)
                .setConnectTimeout(connectTimeoutMillis)
                .setConnectionRequestTimeout(poolRequestTimeoutMillis)
                .build();

        DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(retryCount, false);

        return HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(config)
                .setRetryHandler(retryHandler)
                .build();
    }

    @Bean
    @Qualifier("userBehaviourHystrixConfig")
    public HystrixCommand.Setter userBehaviourHystrixConfig() {
        HystrixPlugins.getInstance().registerMetricsPublisher(
                new RTSHystrixCodaHaleMetricsPublisher(MetricsService.getInstance().getRegistry())
        );
        return HystrixCommandConfigurationProvider.provideUserBehaviourConfig(false);
    }
}
