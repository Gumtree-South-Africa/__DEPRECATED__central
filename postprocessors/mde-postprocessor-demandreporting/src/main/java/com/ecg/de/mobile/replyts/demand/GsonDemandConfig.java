package com.ecg.de.mobile.replyts.demand;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Currency;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Helper to programmatically configure our gson.
 */
public final class GsonDemandConfig {

    public static Gson createConfiguredGson() {
        return new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Map.Entry.class, MapEntrySerializer.INSTANCE)
            .registerTypeAdapter(Date.class, DateTypeAdapter.nullSafe())
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
            //.setPrettyPrinting()
            .create();
    }

    enum MapEntrySerializer implements JsonSerializer<Map.Entry<String,?>> {
        INSTANCE;

        @Override
        public JsonElement serialize(Map.Entry<String, ?> src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject o =  new JsonObject();
            o.add(src.getKey(), context.serialize(src.getValue()));
            return o;
        }
    }

    static final TypeAdapter<Date> DateTypeAdapter = new TypeAdapter<Date>() {
        public Date read(JsonReader in) throws IOException {
            return new Date(in.nextLong() * 1000L);
        }

        public void write(JsonWriter out, Date value) throws IOException {
            out.value(value.getTime() / 1000L);
        }
    };
}
