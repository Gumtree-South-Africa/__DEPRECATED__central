package com.ecg.replyts.commonattributefilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class ExpiringRegEx {
    private String text;

    private Pattern pattern;

    private long expirationTime;

    private long timeoutMillis;

    private String conversationId;

    private String messageId;

    public ExpiringRegEx(String text, Pattern pattern, long timeoutMillis, String conversationId, String messageId) {
        this.text = text;
        this.pattern = pattern;
        this.timeoutMillis = timeoutMillis;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.expirationTime = System.currentTimeMillis() + timeoutMillis;
    }

    public Matcher createMatcher() {
        return pattern.matcher(new ExpiringCharSequence(text));
    }

    private class ExpiringCharSequence implements CharSequence {
        private CharSequence text;

        public ExpiringCharSequence(CharSequence text) {
            this.text = text;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            if (System.currentTimeMillis() > expirationTime) {
                throw new RuntimeException(format("Matcher expired after %d ms with pattern '%s' on conv/message '%s/%s'", timeoutMillis, pattern, conversationId, messageId ));
            }

            return text.charAt(index);
        }

        @Override
        public String toString() {
            return text == null ? "" : text.toString();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ExpiringCharSequence(text.subSequence(start, end));
        }
    }
}