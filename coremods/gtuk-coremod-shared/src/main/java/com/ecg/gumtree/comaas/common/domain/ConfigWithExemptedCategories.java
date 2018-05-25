package com.ecg.gumtree.comaas.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public abstract class ConfigWithExemptedCategories extends CommonConfig {
    @JsonProperty("exemptedCategories")
    private List<Long> exemptedCategories;

    protected ConfigWithExemptedCategories() {
    }

    protected ConfigWithExemptedCategories(Builder builder) {
        super(builder);
        this.exemptedCategories = builder.exemptedCategories;
    }

    public List<Long> getExemptedCategories() {
        return exemptedCategories;
    }

    public abstract static class Builder<ConfigClass extends ConfigWithExemptedCategories, ConfigBuilder extends Builder>
            extends CommonConfig.Builder<ConfigClass, ConfigBuilder> {
        private List<Long> exemptedCategories = new ArrayList<>();

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public ConfigBuilder withExemptedCategories(List<Long> exemptedCategories) {
            this.exemptedCategories = exemptedCategories;
            return (ConfigBuilder) this;
        }
    }

}
