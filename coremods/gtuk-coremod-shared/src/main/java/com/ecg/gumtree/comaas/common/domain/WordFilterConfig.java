package com.ecg.gumtree.comaas.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public final class WordFilterConfig extends ConfigWithExemptedCategories {
    @JsonProperty("rules")
    private List<Rule> rules;

    private WordFilterConfig() {}
    private WordFilterConfig(Builder builder) {
        super(builder);
        this.rules = builder.rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<WordFilterConfig, Builder> {
        private List<Rule> rules = new ArrayList<>();

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public Builder withRules(List<Rule> rules) {
            this.rules = rules;
            return this;
        }

        public WordFilterConfig build() {
            return new WordFilterConfig(this);
        }
    }
}