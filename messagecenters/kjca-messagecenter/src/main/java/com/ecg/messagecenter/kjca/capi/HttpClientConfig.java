package com.ecg.messagecenter.kjca.capi;

public final class HttpClientConfig {

    private final int connectionTimeout;
    private final int connectionManagerTimeout;
    private final int socketTimeout;
    private final int maxConnections;
    private final int retryCount;

    public HttpClientConfig(int connectionTimeout, int connectionManagerTimeout, int socketTimeout, int maxConnections, int retryCount) {
        this.connectionTimeout = connectionTimeout;
        this.connectionManagerTimeout = connectionManagerTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
        this.retryCount = retryCount;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getConnectionManagerTimeout() {
        return connectionManagerTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
