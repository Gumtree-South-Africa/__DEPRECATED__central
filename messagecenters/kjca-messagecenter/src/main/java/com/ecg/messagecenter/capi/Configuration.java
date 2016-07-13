package com.ecg.messagecenter.capi;

public final class Configuration {
    public CommonApi commonApi;
    public HttpClient httpClient;

    public static final class CommonApi {
        public String hostname;
        public Integer port;
        public String username;
        public String password;
    }

    public static final class HttpClient {
        public Integer connectionTimeout;
        public Integer connectionManagerTimeout;
        public Integer socketTimeout;
        public Integer maxConnections;
        public Integer retryCount;
    }

    public Configuration(String hostname, Integer port, String username, String password,
                         Integer connectionTimeout, Integer connectionManagerTimeout, Integer socketTimeout,
                         Integer maxTotalConnections, Integer retryCount) {
        commonApi = new CommonApi();
        commonApi.hostname = hostname;
        commonApi.port = port;
        commonApi.username = username;
        commonApi.password = password;

        httpClient = new HttpClient();
        httpClient.connectionTimeout = connectionTimeout;
        httpClient.connectionManagerTimeout = connectionManagerTimeout;
        httpClient.socketTimeout = socketTimeout;
        httpClient.maxConnections = maxTotalConnections;
        httpClient.retryCount = retryCount;
    }
}
