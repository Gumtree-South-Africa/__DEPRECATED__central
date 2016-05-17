package com.ecg.messagecenter.util;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;

public class MessageCenterUtils {
    public static String truncateText(String description, int maxChars) {

        if (isNullOrEmpty(description))
            return "";

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
