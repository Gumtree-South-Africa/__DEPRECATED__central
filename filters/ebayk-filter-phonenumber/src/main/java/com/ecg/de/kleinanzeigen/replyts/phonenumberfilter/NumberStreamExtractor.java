package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;

import java.util.List;

public class NumberStreamExtractor {

    // Use this instead of Java isDigit(), because it consider lots of exotic characters not useful for phone numbers.
    private static final CharMatcher IS_NUMBER_MATCHER = CharMatcher.inRange('0', '9');
    private static final CharMatcher NEW_LINE_MATCHER = CharMatcher.anyOf("\n\r");
    private static final CharMatcher NORMALIZER_0 = CharMatcher.anyOf("Oo");
    private static final CharMatcher NORMALIZER_1 = CharMatcher.anyOf("Il");

    private final int maxNumberOfSkippableCharacters;
    private final int minLengthOfGroup;

    public NumberStreamExtractor(int maxNumberOfSkippableCharacters, int minLengthOfGroup) {
        this.maxNumberOfSkippableCharacters = maxNumberOfSkippableCharacters;
        this.minLengthOfGroup = minLengthOfGroup;
    }

    public NumberStream extractStream(String text) {
        return new Builder().extractStream(text);
    }

    private class Builder {

        private int skippedCharacters;
        private final List<String> items = Lists.newArrayList();
        private final StringBuilder currentGroup = new StringBuilder();

        public NumberStream extractStream(String text) {
            String normalized = normalizeText(text);
            for (char c : normalized.toCharArray()) {
                if (IS_NUMBER_MATCHER.matches(c)) {
                    appendCurrent(c);
                } else if (NEW_LINE_MATCHER.matches(c)) {  // on new line (LF, CR) complete group
                    completeGroup();
                } else if (!Character.isWhitespace(c)) {  // generally: skip whitespace
                    skipCharacter();
                }
            }
            completeGroup();
            return new NumberStream(items);
        }

        /**
         * Replace similar looking characters with the numbers.
         */
        private String normalizeText(String text) {
            String normalized = NORMALIZER_0.replaceFrom(text, "0");
            return NORMALIZER_1.replaceFrom(normalized, "1");
        }

        private void skipCharacter() {
            ++skippedCharacters;
            if (skippedCharacters > maxNumberOfSkippableCharacters) {
                completeGroup();
            }
        }

        private void completeGroup() {
            if (currentGroup.length() >= minLengthOfGroup) {
                items.add(currentGroup.toString());
            }
            currentGroup.setLength(0);
        }

        private void appendCurrent(char c) {
            skippedCharacters = 0;
            currentGroup.append(c);
        }
    }


}
