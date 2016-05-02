package com.ecg.de.ebayk.messagecenter.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ConverterUtils {
    public static Long nullSafeMillis(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.getMillis();
    }

    public static DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now(DateTimeZone.UTC).withMillis(jsonNode.asLong());
    }
}
