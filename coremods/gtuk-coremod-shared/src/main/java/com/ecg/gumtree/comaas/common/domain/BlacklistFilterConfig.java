package com.ecg.gumtree.comaas.common.domain;

import java.util.ArrayList;
import java.util.List;

public final class BlacklistFilterConfig extends ConfigWithExemptedCategories {

    private String accountHolderHeaderValue;
    private boolean api;
    private int apiConnectionTimeout;
    private String buyerGoodHeader;
    private int maxConnectionsPerRoute;
    private String sellerGoodHeader;
    private boolean stub;
    private List<String> stubBlacklistedUsers;
    private String webServiceUrl;

    private BlacklistFilterConfig(){}

    private BlacklistFilterConfig(Builder builder) {
        super(builder);
        this.accountHolderHeaderValue = builder.accountHolderHeaderValue;
        this.api = builder.api;
        this.apiConnectionTimeout = builder.apiConnectionTimeout;
        this.buyerGoodHeader = builder.buyerGoodHeader;
        this.maxConnectionsPerRoute = builder.maxConnectionsPerRoute;
        this.sellerGoodHeader = builder.sellerGoodHeader;
        this.stub = builder.stub;
        this.stubBlacklistedUsers = builder.stubBlacklistedUsers;
        this.webServiceUrl = builder.webServiceUrl;

    }

    public String getAccountHolderHeaderValue() {
        return accountHolderHeaderValue;
    }

    public boolean isApi() {
        return api;
    }

    public int getApiConnectionTimeout() {
        return apiConnectionTimeout;
    }

    public String getBuyerGoodHeader() {
        return buyerGoodHeader;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public String getSellerGoodHeader() {
        return sellerGoodHeader;
    }

    public boolean isStub() {
        return stub;
    }

    public List<String> getStubBlacklistedUsers() {
        return stubBlacklistedUsers;
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<BlacklistFilterConfig, Builder> {
        private final String accountHolderHeaderValue;
        private final boolean api;
        private final int apiConnectionTimeout;
        private final String buyerGoodHeader;
        private final int maxConnectionsPerRoute;
        private final String sellerGoodHeader;
        private boolean stub;
        private List<String> stubBlacklistedUsers = new ArrayList<>();
        private final String webServiceUrl;

        //CHECKSTYLE:OFF
        public Builder(State state,
                       int priority,
                       Result result,
                       String accountHolderHeaderValue,
                       boolean api,
                       int apiConnectionTimeout,
                       int maxConnectionsPerRoute,
                       String buyerGoodHeader,
                       String sellerGoodHeader,
                       String webServiceUrl) {
            super(state, priority, result);
            this.accountHolderHeaderValue = accountHolderHeaderValue;
            this.api = api;
            this.apiConnectionTimeout = apiConnectionTimeout;
            this.buyerGoodHeader = buyerGoodHeader;
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            this.sellerGoodHeader = sellerGoodHeader;
            this.webServiceUrl = webServiceUrl;
        }
        //CHECKSTYLE:ON

        public Builder withStub(boolean stub) {
            this.stub = stub;
            return this;
        }

        public Builder withStubBlacklistedUsers(List<String> stubBlacklistedUsers) {
            this.stubBlacklistedUsers = stubBlacklistedUsers;
            return this;
        }

        public BlacklistFilterConfig build() {
            return new BlacklistFilterConfig(this);
        }
    }
}