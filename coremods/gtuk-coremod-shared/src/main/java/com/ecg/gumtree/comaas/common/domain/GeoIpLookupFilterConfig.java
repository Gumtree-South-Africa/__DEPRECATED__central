package com.ecg.gumtree.comaas.common.domain;

import java.util.Map;

public final class GeoIpLookupFilterConfig extends CommonConfig {

    private String appName;
    private String proxyUrl;
    private boolean stub;
    private String webServiceUrl;
    private Map<String, String> stubGeoIpCountry;
    private String iafToken;

    private GeoIpLookupFilterConfig() {}
    private GeoIpLookupFilterConfig(Builder builder) {
        super(builder);
        this.appName = builder.appName;
        this.proxyUrl = builder.proxyUrl;
        this.stub = builder.stub;
        this.webServiceUrl = builder.webServiceUrl;
        this.stubGeoIpCountry = builder.stubGeoIpCountry;
        this.iafToken = builder.iafToken;
    }

    public String getAppName() {
        return appName;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public boolean isStub() {
        return stub;
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public Map<String, String> getStubGeoIpCountry() {
        return stubGeoIpCountry;
    }

    public String getIafToken() {
        return iafToken;
    }

    public static final class Builder extends CommonConfig.Builder<GeoIpLookupFilterConfig, Builder> {
        private String appName;
        private String proxyUrl;
        private boolean stub;
        private String webServiceUrl;
        private Map<String, String> stubGeoIpCountry;
        private String iafToken;

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public Builder withAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder withProxyUrl(String proxyUrl) {
            this.proxyUrl = proxyUrl;
            return this;
        }

        public Builder withStub(boolean stub) {
            this.stub = stub;
            return this;
        }

        public Builder withWebServiceUrl(String webServiceUrl) {
            this.webServiceUrl = webServiceUrl;
            return this;
        }

        public Builder withStubGeoIpCountry(Map<String, String> stubGeoIpCountry) {
            this.stubGeoIpCountry = stubGeoIpCountry;
            return this;
        }

        public Builder withIafToken(String iafToken) {
            this.iafToken = iafToken;
            return this;
        }

        public GeoIpLookupFilterConfig build() {
            return new GeoIpLookupFilterConfig(this);
        }
    }

}
