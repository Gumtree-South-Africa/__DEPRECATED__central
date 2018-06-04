package com.ecg.mde.filter.badword;

import java.util.List;

public class FilterResult {

    private final List<BadwordDTO> badwords;

    private final EmailPhoneUrlFilterResult emailPhoneUrlFilterResult;

    public List<BadwordDTO> getBadwords() {
        return badwords;
    }

    public EmailPhoneUrlFilterResult getEmailPhoneUrlFilterResult() {
        return emailPhoneUrlFilterResult;
    }

    public boolean isEmpty() {
        return badwords.isEmpty() &&
                !emailPhoneUrlFilterResult.containsURL() &&
                !emailPhoneUrlFilterResult.containsPhone() &&
                !emailPhoneUrlFilterResult.containsEmail();
    }

    private FilterResult(Builder builder) {
        badwords = builder.badwords;
        emailPhoneUrlFilterResult = builder.emailPhoneUrlFilterResult;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FilterResult copy) {
        Builder builder = new Builder();
        builder.badwords = copy.badwords;
        builder.emailPhoneUrlFilterResult = copy.emailPhoneUrlFilterResult;
        return builder;
    }

    public static final class Builder {
        private List<BadwordDTO> badwords;
        private EmailPhoneUrlFilterResult emailPhoneUrlFilterResult;

        private Builder() {
        }

        public Builder badwords(List<BadwordDTO> val) {
            badwords = val;
            return this;
        }

        public Builder emailPhoneUrlFilterResult(EmailPhoneUrlFilterResult val) {
            emailPhoneUrlFilterResult = val;
            return this;
        }

        public FilterResult build() {
            return new FilterResult(this);
        }
    }
}
