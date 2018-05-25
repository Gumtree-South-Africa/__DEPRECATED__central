package com.ecg.gumtree.comaas.common.domain;

import java.util.ArrayList;
import java.util.List;

public final class WatchlistFilterConfig extends ConfigWithExemptedCategories {

    private boolean api;
    private int apiConnectionTimeout;
    private int maxConnectionsPerRoute;
    private boolean stub;
    private List<String> stubWatchlistedUsers;
    private String webServiceUrl;

    private WatchlistFilterConfig() {}
    private WatchlistFilterConfig(Builder builder) {
        super(builder);
        this.api = builder.api;
        this.apiConnectionTimeout = builder.apiConnectionTimeout;
        this.maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
        this.stub = builder.stub;
        this.stubWatchlistedUsers = builder.stubWatchlistedUsers;
        this.webServiceUrl = builder.webServiceUrl;

    }

    public boolean isApi() {
        return api;
    }

    public int getApiConnectionTimeout() {
        return apiConnectionTimeout;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public boolean isStub() {
        return stub;
    }

    public List<String> getStubWatchlistedUsers() {
        return stubWatchlistedUsers;
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<WatchlistFilterConfig, Builder> {
        private final boolean api;
        private final int apiConnectionTimeout;
        private final int maxConnectionsPerRoute;
        private boolean stub;
        private List<String> stubWatchlistedUsers = new ArrayList<>();
        private final String webServiceUrl;

        public Builder(State state,
                       int priority,
                       Result result,
                       boolean api,
                       int apiConnectionTimeout,
                       int maxConnectionsPerRoute,
                       String webServiceUrl) {
            super(state, priority, result);
            this.api = api;
            this.apiConnectionTimeout = apiConnectionTimeout;
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            this.webServiceUrl = webServiceUrl;
        }

        public Builder withStub(boolean stub) {
            this.stub = stub;
            return this;
        }

        public Builder withStubWatchlistedUsers(List<String> stubWatchlistedUsers) {
            this.stubWatchlistedUsers = stubWatchlistedUsers;
            return this;
        }

        public WatchlistFilterConfig build() {
            return new WatchlistFilterConfig(this);
        }
    }
}