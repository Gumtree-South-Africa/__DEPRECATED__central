package com.ecg.comaas.mde.postprocessor.demandreporting;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;

class HttpClientProvider {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientProvider.class);

    private HttpClient client;

    @Value("${replyts.outbound.event.collector.service.maxConnectionsPerRoute}")
    private int maxConnectionsPerRoute = 2;

    @Value("${replyts.outbound.event.collector.service.maxTotalConnections}")
    private int maxTotalConnections = 1000;

    @Value("${replyts.outbound.event.collector.service.connection.timeout.sec:${replyts.outbound.event.collector.service.connection.timeout.ms}}")
    private int connectionTimeout = 4000;

    @Value("${replyts.outbound.event.collector.service.socket.timeout.ms}")
    private int socketTimeout = 10000;

    @Value("${replyts.outbound.event.collector.service.proxy:#{null}}")
    private URI proxyUri;

    synchronized HttpClient provideClient() {
        if (client != null) return client;
        PoolingClientConnectionManager connectionManager = createConnectionManager();
        HttpParams clientParams = new BasicHttpParams();
        clientParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        clientParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
        LOG.info("Set connection timeout to {} ms and socket timeout to {} ms", connectionTimeout, socketTimeout);

        LOG.info("Providing http client with proxyURI: {}", proxyUri);
        if (proxyUri != null && !proxyUri.toString().trim().equals("null")) {
            LOG.info("Configured http client with proxyURI: {}", proxyUri);
            HttpHost proxyHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort());
            clientParams.setParameter(ConnRouteParams.DEFAULT_PROXY, proxyHost);
        } else {
            LOG.info("Http client will be configured without proxy");
        }

        client = new DefaultHttpClient(connectionManager, clientParams);
        return client;
    }

    synchronized void shutdownClient() {
        try {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        } catch (Exception e) {
            LOG.info("Failed to shutdown httpclient", e);
        } finally {
            client = null;
        }
    }

    private PoolingClientConnectionManager createConnectionManager() {
        PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
        mgr.setMaxTotal(maxTotalConnections);
        mgr.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        return mgr;
    }
}
