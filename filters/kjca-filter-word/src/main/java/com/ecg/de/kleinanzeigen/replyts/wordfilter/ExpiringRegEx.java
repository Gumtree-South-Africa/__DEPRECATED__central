package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Creates a regex matcher that throws a RuntimeException after running for
 * more than the preconfigured amount of time.
 */
class ExpiringRegEx {

    private static final int TIMEOUT_CHECK_RATE = 100;
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
        private int count;

        private ExpiringCharSequence(CharSequence text) {
            this.text = text;
        }

        public int length() {
            return text.length();
        }

        public char charAt(int index) {
            // System.currentTimeMillis() is quite expensive. Therefore, only check from time to time the timeout condition.
            // In a micro benchmark this optimization speedup the processing by factor 3 (tested on 1.8.0_40-b27 on MacBook)
            count++;
            boolean checkTimeout = (count % TIMEOUT_CHECK_RATE) == 0;
            if (checkTimeout && System.currentTimeMillis() > expirationTime) {
                throw new RuntimeException(format("Matcher expired after %d ms with pattern '%s' on conv/message '%s/%s'", timeoutMillis, pattern, conversationId, messageId ));
            }
            return text.charAt(index);
        }

        @Override
        public String toString(){

            return text == null ? "" : text.toString();
        }

        public CharSequence subSequence(int start, int end) {
            return new ExpiringCharSequence(text.subSequence(start, end));
        }
    }
}
