package com.ecg.gumtree.comaas.common.domain;

public final class UserFlaggedFilterConfig extends CommonConfig {

    private String buyerFlaggedHeader;
    private String sellerFlaggedHeader;

    private UserFlaggedFilterConfig() {}
    private UserFlaggedFilterConfig(Builder builder) {
        super(builder);
        this.buyerFlaggedHeader = builder.buyerFlaggedHeader;
        this.sellerFlaggedHeader = builder.sellerFlaggedHeader;
    }

    public String getBuyerFlaggedHeader() {
        return buyerFlaggedHeader;
    }

    public String getSellerFlaggedHeader() {
        return sellerFlaggedHeader;
    }

    public static final class Builder extends CommonConfig.Builder<UserFlaggedFilterConfig, Builder> {
        private final String buyerFlaggedHeader;
        private final String sellerFlaggedHeader;

        public Builder(State state,
                       int priority,
                       Result result,
                       String buyerFlaggedHeader,
                       String sellerFlaggedHeader) {
            super(state, priority, result);
            this.buyerFlaggedHeader = buyerFlaggedHeader;
            this.sellerFlaggedHeader = sellerFlaggedHeader;
        }

        public UserFlaggedFilterConfig build() {
            return new UserFlaggedFilterConfig(this);
        }
    }
}