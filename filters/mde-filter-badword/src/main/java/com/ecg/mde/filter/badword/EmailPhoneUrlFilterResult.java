package com.ecg.mde.filter.badword;

public class EmailPhoneUrlFilterResult {

    private final boolean containsPhone;

    private final boolean containsURL;

    private final boolean containsEmail;

    public boolean containsPhone() {
        return containsPhone;
    }

    public boolean containsURL() {
        return containsURL;
    }

    public boolean containsEmail() {
        return containsEmail;
    }

    private EmailPhoneUrlFilterResult(Builder builder) {
        containsPhone = builder.containsPhone;
        containsURL = builder.containsURL;
        containsEmail = builder.containsEmail;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(EmailPhoneUrlFilterResult copy) {
        Builder builder = new Builder();
        builder.containsPhone = copy.containsPhone;
        builder.containsURL = copy.containsURL;
        builder.containsEmail = copy.containsEmail;
        return builder;
    }


    public static final class Builder {
        private boolean containsPhone;
        private boolean containsURL;
        private boolean containsEmail;

        private Builder() {
        }

        public Builder containsPhone(boolean val) {
            containsPhone = val;
            return this;
        }

        public Builder containsURL(boolean val) {
            containsURL = val;
            return this;
        }

        public Builder containsEmail(boolean val) {
            containsEmail = val;
            return this;
        }

        public EmailPhoneUrlFilterResult build() {
            return new EmailPhoneUrlFilterResult(this);
        }
    }
}