package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 *
 */
class ExpiringRegEx {

    private final String text;
    private final Pattern pattern;
    private final long expirationTime;
    private final long timeoutMillis;
    private final String conversationId;
    private final String messageId;

    public ExpiringRegEx(String text, Pattern pattern, long timeoutMillis, String conversationId, String messageId) {
        this.text = text;
        this.pattern = pattern;
        this.timeoutMillis = timeoutMillis;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.expirationTime = System.currentTimeMillis() + timeoutMillis;
    }

    public Matcher createMatcher() {
        CharSequence charSequence = new ExpiringCharSequence(text);
        return pattern.matcher(charSequence);
    }

    private class ExpiringCharSequence implements CharSequence {
        private final CharSequence text;

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

        public String toString(){

            return text == null ? "" : text.toString();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new ExpiringCharSequence(text.subSequence(start, end));
        }
    }
}
