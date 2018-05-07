package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

public final class GsonConfigurer {

    public static Gson instance() {
        return GsonHolder.INSTANCE;
    }

    private static class GsonHolder {
        private static final TypeAdapter<Date> DateTypeAdapter = new TypeAdapter<Date>() {
            public Date read(JsonReader in) throws IOException {
                return new Date(in.nextLong() * 1000L);
            }

            public void write(JsonWriter out, Date value) throws IOException {
                out.value(value.getTime() / 1000L);
            }
        };

        private static final Gson INSTANCE = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(Map.Entry.class, MapEntrySerializer.INSTANCE)
                .registerTypeAdapter(Date.class, DateTypeAdapter.nullSafe())
                .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
                //.setPrettyPrinting()
                .create();

        private enum MapEntrySerializer implements JsonSerializer<Map.Entry<String, ?>> {
            INSTANCE;

            @Override
            public JsonElement serialize(Map.Entry<String, ?> src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject o = new JsonObject();
                o.add(src.getKey(), context.serialize(src.getValue()));
                return o;
            }
        }
    }
}
