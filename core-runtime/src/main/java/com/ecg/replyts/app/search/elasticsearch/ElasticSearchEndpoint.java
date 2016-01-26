
package com.ecg.replyts.app.search.elasticsearch;

public class ElasticSearchEndpoint {
    private String host;
    private int port;

    public ElasticSearchEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

}
