package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMultimap;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

class HeaderDecoder {

    private static final List<String> NORMALIZED_HEADER_NAMES = Arrays.asList(
            Mail.ADID_HEADER, "MIME-Version"
    );

    public ImmutableMultimap<String, String> decodeHeaders(Message mail) {
        ImmutableMultimap.Builder<String, String> resultMapBdr = ImmutableMultimap.builder();
        List<Field> headerFields = mail.getHeader().getFields();
        for (Field f : headerFields) {
            String normalizedName = normalizeHeaderName(f.getName());
            String value = decodeRfc2047(f.getBody());
            resultMapBdr.put(normalizedName, value);
        }
        return resultMapBdr.build();
    }

    private String decodeRfc2047(String value) {
        try {
            return MimeUtility.decodeText(value);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * This method will rewrite the headers of <code>mail</code>. Each header will get the following treatment:
     * <ul>
     * <li>When header name is in white list (case insensitive), it wil be changed to the name in
     * the white list.
     * <strong>Otherwise</strong></strong></li>
     * <li>First letter of the name is changed to upper case</li>
     * <li>First letter after a '-' is changed to upper case</li>
     * <li>Other letters are lower cased</li>
     * <li>"-Id" at the end will be changed to "-ID"</li>
     * </ul>
     */
    String normalizeHeaderName(String rawHeaderName) {
        for (String normalizedHeaderName : NORMALIZED_HEADER_NAMES) {
            if (normalizedHeaderName.equalsIgnoreCase(rawHeaderName)) return normalizedHeaderName;
        }
        StringBuilder sb = new StringBuilder(rawHeaderName);
        boolean expectNewWord = true;
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            boolean isLetter = CharMatcher.JAVA_LETTER.matches(c);
            boolean isStartOfNewWord = isLetter && expectNewWord;
            if (isStartOfNewWord) {
                // start of a new word - uppercase
                sb.setCharAt(i, Character.toUpperCase(c));
                expectNewWord = false;
            } else if (isLetter) {
                sb.setCharAt(i, Character.toLowerCase(c));
            } else {
                // this is a non alphabethic char like a dash - the next letter to come is the start of a new word
                expectNewWord = true;

            }


        }
        return uppercaseTrailingId(sb);
    }

    private String uppercaseTrailingId(StringBuilder sb) {
        String normalized = sb.toString();
        if (normalized.endsWith("-Id")) {
            sb.setCharAt(sb.length() - 1, 'D');
            return sb.toString();
        }
        return normalized;
    }
}
