package com.ecg.comaas.mde.listener.pushnotification;

import com.ecg.comaas.mde.listener.pushnotification.cassandra.CassandraMessageRepository;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.net.URI;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
@Import({NotificationSender.class, MdePushNotificationListener.class, CassandraMessageRepository.class})
public class MdePushNotificationConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MdePushNotificationConfiguration.class);

    @Value("${replyts.outbound.push.notification.service.maxConnectionsPerRoute:5}")
    private int maxConnectionsPerRoute;

    @Value("${replyts.outbound.push.notification.service.maxTotalConnections:1000}")
    private int maxTotalConnections;

    @Value("${replyts.outbound.push.notification.service.connection.timeout.sec:${replyts.outbound.push.notification.service.connection.timeout.ms:4000}}")
    private int connectionTimeout;

    @Value("${replyts.outbound.push.notification.service.socket.timeout.ms:10000}")
    private int socketTimeout;

    @Value("${replyts.outbound.push.notification.service.proxy:#{null}}")
    private URI proxyUri;

    @Bean
    public HttpClient provideClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        RequestConfig.Builder builder = RequestConfig.custom()
                .setConnectionRequestTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout);

        LOG.info("Set connection timeout to {} ms and socket timeout to {} ms", connectionTimeout, socketTimeout);

        if (proxyUri != null && !proxyUri.toString().trim().equals("null")) {
            LOG.info("Configured http client with proxyURI: {}", proxyUri);
            HttpHost proxyHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort());
            builder.setProxy(proxyHost);
        } else {
            LOG.info("Http client will be configured without proxy");
        }

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(builder.build())
                .setConnectionManager(connectionManager)
                .build();
    }
}
