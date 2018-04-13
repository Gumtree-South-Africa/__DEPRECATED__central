package com.ecg.comaas.kjca.listener.dw.json;

import com.ecg.comaas.kjca.listener.dw.json.helper.DateTimeDeserializer;
import com.ecg.comaas.kjca.listener.dw.json.helper.DateTimeSerializer;
import com.ecg.comaas.kjca.listener.dw.json.helper.ImmutableListDeserializer;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joda.time.DateTime;

public class JsonTransformer<T> {

    private final Class targetType;
    private final Gson gson;

    public JsonTransformer(Class targetType) {
        this.targetType = targetType;
        gson = buildGson(true);
    }

    public JsonTransformer(Class targetType, boolean usePrettyPrinting) {
        this.targetType = targetType;
        gson = buildGson(usePrettyPrinting);
    }

    public String toJson(T target) {
        return gson.toJson(target);
    }

    public <T> T fromJson(String json) {
        return (T) gson.fromJson(json, targetType);
    }

    private Gson buildGson(boolean usePrettyPrinting) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .registerTypeAdapter(ImmutableList.class, new ImmutableListDeserializer())
                .registerTypeAdapter(DateTime.class, new DateTimeSerializer())
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .serializeNulls();

        if (usePrettyPrinting) {
            gsonBuilder.setPrettyPrinting();
        }

        return gsonBuilder.create();
    }
}
