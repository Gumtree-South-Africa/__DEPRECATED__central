package com.ecg.replyts.autogatemaildelivery;

import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * builds a new http client 4 instance with all supplied parameters. It will assume these parametesr as default:
 * <ul>
 * <li>Max Connections: {@value #DEFAULT_MAX_CONNS}</li>
 * <li>Max Connections per route: {@value #DEFAULT_MAX_CONNS_PER_ROUTE}</li>
 * <li>Connection Pool Timeout: {@value #DEFAULT_POOL_TIMEOUT} ms</li>
 * <li>Connection Timeout: {@value #DEFAULT_CONN_TIMEOUT} ms</li>
 * <li>Socket Timeout: {@value #DEFAULT_SO_TIMEOUT} ms</li>
 * <li>No Proxy</li>
 * </ul>
 *
 * @author mhuttar
 */
final class HttpClientBuilder {

    private static final int DEFAULT_MAX_CONNS = 100, DEFAULT_MAX_CONNS_PER_ROUTE = 50, DEFAULT_SO_TIMEOUT = 4000,
            DEFAULT_CONN_TIMEOUT = 4000, DEFAULT_POOL_TIMEOUT = 4000;

    private static final String CONNECTION_MANAGER_TIMEOUT_PARAM_NAME = "http.connection-manager.timeout";


    private static final Logger LOG = LoggerFactory.getLogger(HttpClientBuilder.class);

    private int maxConnections = DEFAULT_MAX_CONNS;
    private int maxConnectionsPerRoute = DEFAULT_MAX_CONNS_PER_ROUTE;
    private int connectionTimeout = DEFAULT_CONN_TIMEOUT;
    private int socketTimeout = DEFAULT_SO_TIMEOUT;
    private int connectionPoolTimeout = DEFAULT_POOL_TIMEOUT;

    private HttpHost proxy = null;

    private HttpClientBuilder() {

    }

    /**
     * limits the maxiumum number of concurrent connections
     *
     * @param total      total number of max connections
     * @param maxPerHost max allowed connections to one host.
     */
    public HttpClientBuilder withConnectionsLimitedTo(int total, int maxPerHost) {
        this.maxConnections = total;
        this.maxConnectionsPerRoute = maxPerHost;
        return this;
    }

    /**
     * defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or, put
     * differently, a maximum period inactivity between two consecutive data packets). A timeout value of zero is
     * interpreted as an infinite timeout.
     */
    public HttpClientBuilder withSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * determines the timeout in milliseconds until a connection is established. A timeout value of zero is interpreted
     * as an infinite timeout
     */
    public HttpClientBuilder withConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    /**
     * configures proxy server to be used for all requests made by this Http Client.
     */
    public HttpClientBuilder usingProxy(String host, int port) {
        proxy = new HttpHost(host, port);
        return this;
    }


    /**
     * timeout for waiting for a free connection from the http client connection pool.
     */
    public HttpClientBuilder withConnectionPoolTimeout(int connPoolTimeout) {
        this.connectionPoolTimeout = connPoolTimeout;
        return this;
    }


    /**
     * @return configured httpclient.
     */
    public HttpClient build() {
        LOG.debug("Building HttpClient 4");
        ThreadSafeClientConnManager connMgr = new ThreadSafeClientConnManager(generateSchemeRegistry());
        connMgr.setMaxTotal(maxConnections);
        connMgr.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        LOG.debug("Connections Max: {}, Max Per Route: {}", maxConnections, maxConnectionsPerRoute);
        DefaultHttpClient client = new DefaultHttpClient(connMgr, generateHttpParams());

        if (proxy != null) {
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        return client;
    }

    private SchemeRegistry generateSchemeRegistry() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));
        return schemeRegistry;
    }

    private BasicHttpParams generateHttpParams() {
        BasicHttpParams params = new BasicHttpParams();

        LOG.debug("Protocol: HTTP 1.1");
        LOG.debug("Socket Timeout: {} ms", socketTimeout);
        LOG.debug("Connection Timeout: {} ms", connectionTimeout);
        LOG.debug("Connection Pool Timeout: {} ms", connectionPoolTimeout);

        params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        params.setParameter(CONNECTION_MANAGER_TIMEOUT_PARAM_NAME, connectionPoolTimeout);

        return params;
    }

    /**
     * @return new builder instance
     */
    public static HttpClientBuilder createHttpclient() {
        return new HttpClientBuilder();
    }
}