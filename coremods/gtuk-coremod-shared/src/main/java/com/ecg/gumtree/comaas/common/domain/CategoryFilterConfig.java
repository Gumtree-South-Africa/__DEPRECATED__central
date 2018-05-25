package com.ecg.gumtree.comaas.common.domain;

public final class CategoryFilterConfig extends CommonConfig {

    private String webServiceUrl;

    private CategoryFilterConfig() {
    }

    private CategoryFilterConfig(Builder builder) {
        super(builder);
        this.webServiceUrl = builder.webServiceUrl;
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public static final class Builder extends CommonConfig.Builder<CategoryFilterConfig, Builder> {
        private final String webServiceUrl;

        public Builder(State state, int priority, Result result, String webServiceUrl) {
            super(state, priority, result);
            this.webServiceUrl = webServiceUrl;
        }

        public CategoryFilterConfig build() {
            return new CategoryFilterConfig(this);
        }
    }
}