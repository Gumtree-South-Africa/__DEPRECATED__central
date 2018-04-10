package com.ecg.comaas.ebayk.filter.emailaddress;

import com.google.common.base.CharMatcher;

import java.util.regex.Pattern;

// From http://www.ietf.org/rfc/rfc822.txt
// 3.3.  LEXICAL TOKENS
public class TextNormalizer {

    // Note: exclude ")" and "(" before and after at pattern to keep comments brackets!
    private static final Pattern AT_PATTERN = Pattern.compile("[^a-zA-Z0-9)]{1,5}((@)|(©)|(®)|(at)|(ät)|(et))[^a-zA-Z0-9(]{1,5}");
    private static final Pattern AT_PATTERN_2 = Pattern.compile("[^a-zA-Z0-9)]{0,3}((©)|(®)|(\\(a?\\)))[^a-zA-Z0-9(]{0,3}");
    private static final Pattern DOT_PATTERN = Pattern.compile("[^a-zA-Z0-9)]{1,5}((\\.)|(dot)|(pkt)|(punkt))[^a-zA-Z0-9(]{1,5}");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(\\([^)]*\\))");

    private static final String HTML_ENCODED_AT = "&#064;";

    public String normalize(String text) {

        String cleaned = text.toLowerCase();

        cleaned = cleaned.replaceAll(HTML_ENCODED_AT, "@");

        cleaned = DOT_PATTERN.matcher(cleaned).replaceAll(".");
        cleaned = AT_PATTERN.matcher(cleaned).replaceAll("@");
        cleaned = AT_PATTERN_2.matcher(cleaned).replaceAll("@");
        cleaned = CharMatcher.WHITESPACE.removeFrom(cleaned);
        // Append the complete result with comments removed to have all possible cases of valid email addresses included in the text
        String cleanedWithoutComments = COMMENT_PATTERN.matcher(cleaned).replaceAll("");

        return cleaned + " " +  cleanedWithoutComments;
    }

}
