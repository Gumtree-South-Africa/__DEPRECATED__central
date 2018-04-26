package com.ecg.messagecenter.kjca.capi;

import org.apache.http.HttpHost;

public final class CommonApiConfig {

    private final HttpHost capiEndpoint;
    private final String username;
    private final String password;

    public CommonApiConfig(String capiHost, int capiPort, String capiScheme, String username, String password) {
        this.capiEndpoint = new HttpHost(capiHost, capiPort, capiScheme);
        this.username = username;
        this.password = password;
    }

    public HttpHost getCapiEndpoint() {
        return capiEndpoint;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
