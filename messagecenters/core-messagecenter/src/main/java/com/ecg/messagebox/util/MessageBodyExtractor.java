package com.ecg.messagebox.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MessageBodyExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBodyExtractor.class);
    /**
     * Invisible separator used as marker for text.
     * See http://www.fileformat.info/info/unicode/char/2063/index.htm for more details.
     */
    static final String MARKER = "\u2063";
    private static final Pattern pattern = Pattern.compile(MARKER + "(.*)" + MARKER, Pattern.DOTALL);

    static String extractBodyMarkedByNonPrintableChars(String message) {
        try {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("failed to extract text body using non printable characters: {}", e.getMessage(), e);
        }

        return message;
    }
}
