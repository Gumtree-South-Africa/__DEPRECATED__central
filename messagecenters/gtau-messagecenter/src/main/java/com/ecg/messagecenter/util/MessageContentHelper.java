package com.ecg.messagecenter.util;

import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageContentHelper {
    static final Pattern XML_HEURISTIC_PATTERN = Pattern.compile("^<(.*)>(.*?)</(.*)>", Pattern.DOTALL);
    static final Pattern SENDER_PATTERN = Pattern.compile("(.*)via\\sgumtree$", Pattern.CASE_INSENSITIVE);
    static final String ANONYMOUS = "Anonymous";

    /**
     * Checks if something looks like XML. It uses an heuristic instead of actually validating if something is properly
     * XML because it is cheaper to use the heuristic.
     *
     * An alternative implementation would be to use something like JAXB to validate if something is actually XML.
     * @param text
     * @return
     */
    public static boolean isLooksLikeXml(String text) {
        return StringUtils.hasText(text) && XML_HEURISTIC_PATTERN.matcher(text.trim()).find();
     }

    public static String senderName(String name) {
        return Optional.ofNullable(name).map(new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable String input) {
                if(!StringUtils.hasText(input)) {
                    return ANONYMOUS;
                }

                String result = input;
                Matcher matcher = SENDER_PATTERN.matcher(input);
                if (matcher.matches()) {
                    result = matcher.group(1).trim();
                }
                return !StringUtils.hasText(result) ? ANONYMOUS : result;
            }
        }).get();
    }
}