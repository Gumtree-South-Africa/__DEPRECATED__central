package com.ecg.gumtree.replyts2.common.message;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public final class MessageCenterUtils {

    private static final Pattern REMOVE_DOUBLE_WHITESPACES = Pattern.compile("\\s+");

    private MessageCenterUtils() {
    }

    public static String trimText(String text) {
        return REMOVE_DOUBLE_WHITESPACES.matcher(text).replaceAll(" ");
    }

    public static String truncateText(String description, int maxChars) {
        if (isNullOrEmpty(description)) {
            return "";
        }

        if (description.length() <= maxChars) {
            return description;
        } else {
            String substring = description.substring(0, maxChars);
            return substringBeforeLast(substring, " ").concat("...");
        }
    }

    public static String toFormattedTimeISO8601ExplicitTimezoneOffset(DateTime date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return format.format(date.toDate());
    }

    private static String substringBeforeLast(String str, String separator) {
        if (isNullOrEmpty(str) || isNullOrEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    private static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }
}
