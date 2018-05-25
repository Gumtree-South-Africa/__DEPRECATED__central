package com.ecg.gumtree.comaas.common.domain;

public final class KnownGoodFilterConfig extends ConfigWithExemptedCategories {

    private String buyerGoodHeader;
    private String sellerGoodHeader;

    private KnownGoodFilterConfig() {}
    private KnownGoodFilterConfig(Builder builder) {
        super(builder);
        this.buyerGoodHeader = builder.buyerGoodHeader;
        this.sellerGoodHeader = builder.sellerGoodHeader;
    }

    public String getBuyerGoodHeader() {
        return buyerGoodHeader;
    }

    public String getSellerGoodHeader() {
        return sellerGoodHeader;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<KnownGoodFilterConfig, Builder> {
        private final String buyerGoodHeader;
        private final String sellerGoodHeader;

        public Builder(State state,
                       int priority,
                       Result result,
                       String buyerGoodHeader,
                       String sellerGoodHeader) {
            super(state, priority, result);
            this.buyerGoodHeader = buyerGoodHeader;
            this.sellerGoodHeader = sellerGoodHeader;
        }

        public KnownGoodFilterConfig build() {
            return new KnownGoodFilterConfig(this);
        }
    }
}