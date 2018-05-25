package com.ecg.gumtree.comaas.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class Rule {
    @JsonProperty("exceptions")
    private List<String> exceptions;
    @JsonProperty("pattern")
    private String pattern;
    @JsonProperty("wordBoundaries")
    private boolean wordBoundaries;

    private Rule() {
    }

    private Rule(Builder builder) {
        this.exceptions = builder.exceptions;
        this.pattern = builder.pattern;
        this.wordBoundaries = builder.wordBoundaries;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean getWordBoundaries() {
        return wordBoundaries;
    }


    public static final class Builder {
        private List<String> exceptions;
        private final String pattern;
        private boolean wordBoundaries;

        public Builder(String pattern) {
            this.pattern = pattern;
        }

        public Builder withExceptions(List<String> exceptions) {
            this.exceptions = exceptions;
            return this;
        }

        public Builder withWordBoundaries(boolean wordBoundaries) {
            this.wordBoundaries = wordBoundaries;
            return this;
        }

        public Rule build() {
            return new Rule(this);
        }
    }
}
