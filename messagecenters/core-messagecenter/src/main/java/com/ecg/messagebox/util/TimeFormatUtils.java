package com.ecg.messagebox.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

public class TimeFormatUtils {

    public final static String DATE_FORMAT_STR_ISO8601_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final static DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT_STR_ISO8601_Z);

    private TimeFormatUtils() {
    }

    public static String format(DateTime date) {
        return FORMATTER.print(date);
    }

    public static class DateTimeSerializer extends JsonSerializer<DateTime> {

        @Override
        public void serialize(DateTime value, JsonGenerator gen, SerializerProvider arg2) throws IOException {
            gen.writeString(FORMATTER.print(value));
        }
    }
}