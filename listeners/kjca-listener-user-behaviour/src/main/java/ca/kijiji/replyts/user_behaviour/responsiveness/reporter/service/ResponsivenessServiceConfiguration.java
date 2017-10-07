package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.ServiceDirectory;
import ca.kijiji.discovery.consul.DnsConsulCatalog;
import ca.kijiji.replyts.user_behaviour.responsiveness.hystrix.metrics.RTSHystrixCodaHaleMetricsPublisher;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;

@Configuration
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

    @Value("${consul.host:vm.dev.kjdev.ca}")
    private String host;

    @Value("${consul.port:8600}")
    private int port;

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
    public ServiceDirectory serviceDirectory() {
        try {
            return DnsConsulCatalog.usingUdp(this.host, this.port);
        } catch (final UnknownHostException ex) {
            throw new Error("Unable to create a service catalog.", ex);
        }
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
