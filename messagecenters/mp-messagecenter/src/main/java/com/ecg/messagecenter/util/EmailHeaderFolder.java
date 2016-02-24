package com.ecg.messagecenter.util;

import javax.mail.internet.MimeUtility;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * As per Internet Mail RFC 5322, long header values have to be folded on the sending part and unfolded on the receiving part.
 *
 * @author aneacsu
 * @see <a href="https://tools.ietf.org/html/rfc5322#section-2.2.3">RFC 5322</a>
 */
public class EmailHeaderFolder {

    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\\\n";

    public static String unfold(String headerValue) {
        return isBlank(headerValue) ? headerValue :
                unescapeNewLines(MimeUtility.unfold(headerValue));
    }

    private static String unescapeNewLines(String value) {
        return value.replaceAll(NEW_LINE_CHARACTER_ESCAPED, NEW_LINE_CHARACTER);
    }
}