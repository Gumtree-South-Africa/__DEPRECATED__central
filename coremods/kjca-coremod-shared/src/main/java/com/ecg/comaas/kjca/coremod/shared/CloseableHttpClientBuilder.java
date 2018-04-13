package com.ecg.comaas.kjca.coremod.shared;

import com.google.common.collect.ImmutableList;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;

import java.util.List;

/**
 * Builder for CloseableHttpClient with the most common settings.
 */
public class CloseableHttpClientBuilder {

    private Integer connectionTimeout;
    private Integer connectionManagerTimeout;
    private Integer socketTimeout;
    private String proxyHost;
    private Integer proxyPort;
    private Integer maxPoolSizeTotal;
    private Integer maxPoolSizeDefaultPerRoute;
    private boolean disableContentCompression = false;
    private PoolingHttpClientConnectionManager connectionManager;
    private ServiceUnavailableRetryStrategy retryStrategy;
    private ConnectionKeepAliveStrategy connectionKeepAliveStrategy;
    private HttpRequestRetryHandler httpRequestRetryHandler;
    private List<BasicHeader> defaultHeaders;

    public static CloseableHttpClientBuilder aCloseableHttpClient() {
        return new CloseableHttpClientBuilder();
    }

    public CloseableHttpClientBuilder withConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public CloseableHttpClientBuilder withConnectionManagerTimeout(Integer connectionManagerTimeout) {
        this.connectionManagerTimeout = connectionManagerTimeout;
        return this;
    }

    public CloseableHttpClientBuilder withSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    /**
     * Sets all timeouts to the same value
     */
    public CloseableHttpClientBuilder withTimeout(Integer timeout) {
        return withConnectionManagerTimeout(timeout)
                .withConnectionTimeout(timeout)
                .withSocketTimeout(timeout);
    }

    public CloseableHttpClientBuilder withProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public CloseableHttpClientBuilder withProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public CloseableHttpClientBuilder withMaxPoolSizeTotal(Integer maxPoolSizeTotal) {
        this.maxPoolSizeTotal = maxPoolSizeTotal;
        return this;
    }

    public CloseableHttpClientBuilder withMaxPoolSizeDefaultPerRoute(Integer maxPoolSizeDefaultPerRoute) {
        this.maxPoolSizeDefaultPerRoute = maxPoolSizeDefaultPerRoute;
        return this;
    }

    public CloseableHttpClientBuilder withRetryStrategy(ServiceUnavailableRetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
        return this;
    }

    public CloseableHttpClientBuilder withConnectionKeepAliveStrategy(ConnectionKeepAliveStrategy connectionKeepAliveStrategy) {
        this.connectionKeepAliveStrategy = connectionKeepAliveStrategy;
        return this;
    }

    /**
     * Uses the same size for both the total limit of connections and the default limit per route
     */
    public CloseableHttpClientBuilder withConnectionPoolSize(Integer connectionPoolSize) {
        return withMaxPoolSizeTotal(connectionPoolSize)
                .withMaxPoolSizeDefaultPerRoute(connectionPoolSize);
    }

    /**
     * Use this instead of the connection pool sizes if you need to retain a reference on the connection manager.
     */
    public CloseableHttpClientBuilder withConnectionManager(PoolingHttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        return this;
    }

    public CloseableHttpClientBuilder withDisableContentCompression(boolean disableContentCompression) {
        this.disableContentCompression = disableContentCompression;
        return this;
    }

    public CloseableHttpClientBuilder withHttpRequestRetryHandler(HttpRequestRetryHandler httpRequestRetryHandler) {
        this.httpRequestRetryHandler = httpRequestRetryHandler;
        return this;
    }

    public CloseableHttpClientBuilder withDefaultHeaders(BasicHeader... headers) {
        this.defaultHeaders = ImmutableList.copyOf(headers);
        return this;
    }

    public CloseableHttpClient build() {
        if (connectionManager == null) {
            connectionManager = new PoolingHttpClientConnectionManager();
            if (isPositive(maxPoolSizeTotal)) {
                connectionManager.setMaxTotal(maxPoolSizeTotal);
            }
            if (isPositive(maxPoolSizeDefaultPerRoute)) {
                connectionManager.setDefaultMaxPerRoute(maxPoolSizeDefaultPerRoute);
            }
        }

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        if (isPositive(socketTimeout)) {
            requestConfigBuilder.setSocketTimeout(socketTimeout);
        }
        if (isPositive(connectionTimeout)) {
            requestConfigBuilder.setConnectTimeout(connectionTimeout);
        }
        if (isPositive(connectionManagerTimeout)) {
            requestConfigBuilder.setConnectionRequestTimeout(connectionManagerTimeout);
        }
        RequestConfig requestConfig = requestConfigBuilder.build();

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig);

        if (disableContentCompression) {
            httpClientBuilder.disableContentCompression();
        }

        if (retryStrategy != null) {
            httpClientBuilder.setServiceUnavailableRetryStrategy(retryStrategy);
        }

        if (httpRequestRetryHandler != null) {
            httpClientBuilder.setRetryHandler(httpRequestRetryHandler);
        }

        if (connectionKeepAliveStrategy != null) {
            httpClientBuilder.setKeepAliveStrategy(connectionKeepAliveStrategy);
        }

        if (proxyHost != null && isPositive(proxyPort)) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpClientBuilder.setProxy(proxy);
        }

        if (defaultHeaders != null) {
            httpClientBuilder.setDefaultHeaders(defaultHeaders);
        }

        return httpClientBuilder.build();
    }

    private boolean isPositive(Integer num) {
        return num != null && num > 0;
    }
}
