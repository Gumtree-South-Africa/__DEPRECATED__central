package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters;

import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * User: acharton
 * Date: 12/18/12
 */
@Component
public class HttpClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);

    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionTtlSeconds:120}")
    private int maxConnectionTtlSeconds;

    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionsPerRoute:30}")
    private int maxConnectionsPerRoute;
    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionsTotal:100}")
    private int maxConnections;

    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.connectionTimeoutMs:4000}")
    private int connectionTimeout;
    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.socketTimeoutMs:4000}")
    private int socketTimeout;

    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.proxy.host:}")
    private String proxyHost;

    @Value("${replyts2-ebayservicesfilters-plugin.httpclient.proxy.port:0}")
    private Integer proxyPort;

    private PoolingClientConnectionManager poolingConnectionManager;

    @Bean
    public HttpClient buildClient() {

        LOG.trace("eBay Service Filters. Max Connections (PerRoute/Total): {}/{}. Timeouts (conn/so): {}/{}", maxConnectionsPerRoute, maxConnections, connectionTimeout, socketTimeout);
        SchemeRegistry registry = SchemeRegistryFactory.createDefault();

        poolingConnectionManager = new PoolingClientConnectionManager(registry, maxConnectionTtlSeconds, TimeUnit.SECONDS);
        poolingConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        poolingConnectionManager.setMaxTotal(maxConnections);

        HttpParams clientParams = new BasicHttpParams();
        clientParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        clientParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
        if(!Strings.isNullOrEmpty(proxyHost)) {
            LOG.info("eBay Service Filters. Proxy {}:{}", proxyHost, proxyPort);
            clientParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, proxyPort));
        }

        return new DefaultHttpClient(poolingConnectionManager, clientParams);
    }

    @PreDestroy
    void shutdown() {
        poolingConnectionManager.shutdown();
    }
}
