package com.ecg.gumtree.comaas.common.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public final class ConfigMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setSerializationInclusion(NON_NULL);
    }

    private ConfigMapper() {
    }

    public static String asJson(Object config) throws JsonProcessingException {
        return MAPPER.writeValueAsString(config);
    }

    public static <T> T asObject(String json, Class<T> type) throws IOException {
        return MAPPER.readValue(json, type);
    }
}
