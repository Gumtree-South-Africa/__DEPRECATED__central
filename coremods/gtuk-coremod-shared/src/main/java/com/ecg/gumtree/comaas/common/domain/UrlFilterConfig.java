package com.ecg.gumtree.comaas.common.domain;

import java.util.ArrayList;
import java.util.List;

public final class UrlFilterConfig extends ConfigWithExemptedCategories {
    private List<String> safeUrls;

    private UrlFilterConfig() {}
    private UrlFilterConfig(Builder builder) {
        super(builder);
        this.safeUrls = builder.safeUrls;
    }

    public List<String> getSafeUrls() {
        return safeUrls;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<UrlFilterConfig, Builder> {
        private List<String> safeUrls = new ArrayList<>();

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public Builder withSafeUrls(List<String> safeUrls) {
            this.safeUrls = safeUrls;
            return this;
        }

        public UrlFilterConfig build() {
            return new UrlFilterConfig(this);
        }
    }
}