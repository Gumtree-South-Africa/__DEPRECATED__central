package com.ecg.gumtree.comaas.common.domain;

public final class VelocityFilterConfig extends ConfigWithExemptedCategories {
    private boolean exceeding;
    private int messages;
    private int seconds;
    private int whitelistSeconds;
    private MessageState messageState;
    private FilterField filterField;

    private VelocityFilterConfig() {}
    private VelocityFilterConfig(Builder builder) {
        super(builder);
        this.exceeding = builder.exceeding;
        this.messages = builder.messages;
        this.seconds = builder.seconds;
        this.whitelistSeconds = builder.whitelistSeconds;
        this.messageState = builder.messageState;
        this.filterField = builder.filterField;
    }

    public boolean isExceeding() {
        return exceeding;
    }

    public int getMessages() {
        return messages;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getWhitelistSeconds() {
        return whitelistSeconds;
    }

    public MessageState getMessageState() {
        return messageState;
    }

    public FilterField getFilterField() {
        return filterField;
    }

    public enum FilterField {
        EMAIL, COOKIE, IP_ADDRESS
    }

    public enum MessageState {
        CREATED,
        IGNORED,
        UNPARSEABLE,
        ORPHANED,
        FILTERABLE,
        QUARANTINE,
        HELD,
        BLOCKED,
        SENDABLE,
        SENT,
        GIVENUP,
        _ENDSTATE
    }

    public static final class Builder extends ConfigWithExemptedCategories.Builder<VelocityFilterConfig, Builder> {
        private boolean exceeding;
        private int messages;
        private int seconds;
        private int whitelistSeconds;
        private MessageState messageState;
        private FilterField filterField;

        public Builder(State state, int priority, Result result) {
            super(state, priority, result);
        }

        public Builder withExceeding(boolean exceeding) {
            this.exceeding = exceeding;
            return this;
        }

        public Builder withMessages(int messages) {
            this.messages = messages;
            return this;
        }

        public Builder withSeconds(int seconds) {
            this.seconds = seconds;
            return this;
        }

        public Builder withWhitelistSeconds(int whitelistSeconds) {
            this.whitelistSeconds = whitelistSeconds;
            return this;
        }

        public Builder withMessageState(MessageState messageState) {
            this.messageState = messageState;
            return this;
        }

        public Builder withFilterField(FilterField filterField) {
            this.filterField = filterField;
            return this;
        }

        public VelocityFilterConfig build() {
            return new VelocityFilterConfig(this);
        }
    }
}