package com.gumtree.replyts2.googleanalytics;

import com.gumtree.analytics.event.GAEvent;

public final class ReplyEmailBackendSuccess extends GAEvent {

    private ReplyEmailBackendSuccess(ReplyEmailBackendSuccess.Builder builder) {
        super(builder);
    }

    public static ReplyEmailBackendSuccess.Builder create() {
        return new ReplyEmailBackendSuccess.Builder();
    }

    public static class Builder extends GAEventBuilder<ReplyEmailBackendSuccess> {
        public Builder() {}

        public ReplyEmailBackendSuccess build() {
            return new ReplyEmailBackendSuccess(this);
        }
    }
}
