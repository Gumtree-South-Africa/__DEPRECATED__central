package com.ecg.messagecenter.it.util;

import com.google.common.base.Strings;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageContentHelper {

    private static final Pattern XML_PATTERN = Pattern.compile("<(\\S+?)(.*?)>(.*?)</\\1>");
    private static final Pattern SENDER_PATTERN = Pattern.compile("(.*)via\\sgumtree$", Pattern.CASE_INSENSITIVE);
    private static final String ANONYMOUS = "Anonymous";

    static boolean isXml(String text) {
        return !Strings.isNullOrEmpty(text) && XML_PATTERN.matcher(text).find();
    }

    public static String senderName(String name) {
        return Optional.ofNullable(name).map(input -> {
            if (Strings.isNullOrEmpty(input)) {
                return ANONYMOUS;
            }

            String result = input;
            Matcher matcher = SENDER_PATTERN.matcher(input);
            if (matcher.matches()) {
                result = matcher.group(1).trim();
            }
            return Strings.isNullOrEmpty(result) ? ANONYMOUS : result;
        }).get();
    }
}
