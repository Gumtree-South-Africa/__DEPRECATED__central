package com.ecg.de.mobile.replyts.demand;

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

public class HttpClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientProvider.class);
    private final Config config;
    private HttpClient client;

    public HttpClientProvider(Config config) {
        this.config = config;
    }

    public synchronized HttpClient provideClient() {
        if (client != null) return client;
        PoolingClientConnectionManager connectionManager = createConnectionManager();
        HttpParams clientParams = new BasicHttpParams();
        clientParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, config.connectionTimeout());
        clientParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, config.socketTimeout());

        logger.info("Providing http client with proxyURI: " + config.proxyUri());
        if (config.proxyUri() != null && !config.proxyUri().toString().trim().equals("null")) {
            logger.info("Configured http client with proxyURI: " + config.proxyUri());
            HttpHost proxyHost = new HttpHost(config.proxyUri().getHost(), config.proxyUri().getPort());
            clientParams.setParameter(ConnRouteParams.DEFAULT_PROXY, proxyHost);
        } else {
            logger.info("Http client will be configured without proxy");
        }

        client = new DefaultHttpClient(connectionManager, clientParams);
        return client;
    }

    public synchronized void shutdownClient() {
        try {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        } catch (Exception e) {
            logger.info("Failed to shutdown httpclient", e);
        } finally {
            client = null;
        }
    }

    private PoolingClientConnectionManager createConnectionManager() {
        PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
        mgr.setMaxTotal(config.maxTotalConnections());
        mgr.setDefaultMaxPerRoute(config.maxConnectionsPerRoute());
        return mgr;
    }

    public static class Config {

        @Value("${replyts.outbound.event.collector.service.maxConnectionsPerRoute}")
        private int maxConnectionsPerRoute = 2;

        @Value("${replyts.outbound.event.collector.service.maxTotalConnections}")
        private int maxTotalConnections = 1000;

        @Value("${replyts.outbound.event.collector.service.connection.timeout.sec}")
        private int connectionTimeout = 0;

        @Value("${replyts.outbound.event.collector.service.socket.timeout.ms}")
        private int socketTimeout = 1000;

        @Value("${replyts.outbound.event.collector.service.proxy:null}")
        private URI proxyUri;

        public int connectionTimeout() {
            return connectionTimeout;
        }

        public int maxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        public int maxTotalConnections() {
            return maxTotalConnections;
        }

        public URI proxyUri() {
            return proxyUri;
        }

        public int socketTimeout() {
            return socketTimeout;
        }
    }

}
