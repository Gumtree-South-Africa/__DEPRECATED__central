package com.ecg.comaas.kjca.listener.dw.json.helper;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.lang.reflect.Type;

public class DateTimeDeserializer implements JsonDeserializer<DateTime> {
    public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return new DateTime(json.getAsJsonPrimitive().getAsString(), DateTimeZone.UTC);
    }
}
