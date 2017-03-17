package com.ecg.de.ebayk.messagecenter.pushmessage;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Create HTTP clients. It will be ensured, that the connection pool from created clients will be shutdown on VM shutdown.
 * <p/>
 * NOTE: The factory shouldn't be used in loops because all generated HTTP client instances will be stored in a list for shutdown hook.
 * <p/>
 * User: maldana Date: 05.07.11 Time: 08:42
 *
 * @author maldana@ebay.de
 */
final class HttpClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientBuilder.class);

    private static final String TIMEOUT_PARAM_NAME = "http.connection-manager.timeout";

    private static final List<HttpClient> CREATED_CLIENTS = Collections.synchronizedList(new ArrayList<HttpClient>());

    static {
        // cleanup all connection pools on vm shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (HttpClient client : CREATED_CLIENTS) {
                    // try/catch - prevent loop break
                    try {
                        client.getConnectionManager().shutdown();
                    } catch (RuntimeException e) {
                        LOG.error("error on http client connection manager shutdown", e);
                    }
                }
            }
        });
    }

    private HttpClientBuilder() {
    }

    public static HttpClient buildHttpClient(int connectionTimeout,
                                             int connectionManagerTimeout,
                                             int socketTimeout,
                                             int maxConnectionsPerHost,
                                             int maxTotalConnections) {

        HttpClient httpClient = new DefaultHttpClient(createConnectionManager(maxConnectionsPerHost, maxTotalConnections));

        initParams(httpClient, connectionTimeout, socketTimeout, connectionManagerTimeout);

        CREATED_CLIENTS.add(httpClient);

        return httpClient;
    }

    private static void initParams(HttpClient httpClient, int connectionTimeout, int socketTimeout, int poolTimeout) {
        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        httpClient.getParams().setParameter(TIMEOUT_PARAM_NAME, poolTimeout);
    }

    private static ClientConnectionManager createConnectionManager(int maxConnectionsPerHost, int maxTotalConnections) {
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        cm.setMaxTotal(maxTotalConnections);
        cm.setDefaultMaxPerRoute(maxConnectionsPerHost);
        return cm;
    }

}
