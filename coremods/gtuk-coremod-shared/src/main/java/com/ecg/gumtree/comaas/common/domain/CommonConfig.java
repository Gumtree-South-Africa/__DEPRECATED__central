package com.ecg.gumtree.comaas.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CommonConfig {
    @JsonProperty("state")
    private State state;

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("result")
    private Result result;

    @JsonProperty("version")
    private String version;

    CommonConfig() {

    }

    CommonConfig(Builder builder) {
        this.state = builder.state;
        this.priority = builder.priority;
        this.result = builder.result;
        this.version = builder.version;
    }

    public State getState() {
        return state;
    }

    public int getPriority() {
        return priority;
    }

    public Result getResult() {
        return result;
    }

    public String getVersion() {
        return version;
    }


    public abstract static class Builder<ConfigClass extends CommonConfig, ConfigBuilder extends Builder> {
        private String version;
        private State state = State.DISABLED;
        private final int priority;
        private final Result result;

        public Builder(State state, int priority, Result result) {
            this.state = state;
            this.priority = priority;
            this.result = result;
            this.version = "";
        }

        public ConfigBuilder withVersion(String version) {
            this.version = version;
            return (ConfigBuilder) this;
        }

        public abstract ConfigClass build();
    }
}
