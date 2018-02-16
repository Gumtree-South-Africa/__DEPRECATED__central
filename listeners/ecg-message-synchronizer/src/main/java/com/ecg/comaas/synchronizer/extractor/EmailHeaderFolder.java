package com.ecg.comaas.synchronizer.extractor;

import org.apache.commons.lang3.StringUtils;

import javax.mail.internet.MimeUtility;

/**
 * Copied class from MessageBox {@link com.ecg.messagebox.util.EmailHeaderFolder}
 *
 * As per Internet Mail RFC 5322, long header values have to be folded on the sending part and unfolded on the receiving part.
 *
 * @author aneacsu
 * @see <a href="https://tools.ietf.org/html/rfc5322#section-2.2.3">RFC 5322</a>
 */
class EmailHeaderFolder {

    private static final String NEW_LINE_CHARACTER = "\n";
    private static final String NEW_LINE_CHARACTER_ESCAPED = "\\\\n";

    static String unfold(String headerValue) {
        return StringUtils.isBlank(headerValue) ? headerValue :
                unescapeNewLines(MimeUtility.unfold(headerValue));
    }

    private static String unescapeNewLines(String value) {
        return value.replaceAll(NEW_LINE_CHARACTER_ESCAPED, NEW_LINE_CHARACTER);
    }
}
