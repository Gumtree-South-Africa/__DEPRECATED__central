package com.ecg.gumtree.comaas.common.domain;

import java.util.ArrayList;
import java.util.List;

public final class GeoIpFilterConfig extends ConfigWithExemptedCategories {
    private List<String> countrySet;

    private GeoIpFilterConfig(){}
    private GeoIpFilterConfig(Builder builder) {
        super(builder);
        this.countrySet = builder.countrySet;
    }

    public List<String> getCountrySet() {
        return countrySet;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<GeoIpFilterConfig, Builder> {
        private List<String> countrySet = new ArrayList<>();

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public Builder withCountrySet(List<String> countrySet) {
            this.countrySet = countrySet;
            return this;
        }

        public GeoIpFilterConfig build() {
            return new GeoIpFilterConfig(this);
        }
    }
}