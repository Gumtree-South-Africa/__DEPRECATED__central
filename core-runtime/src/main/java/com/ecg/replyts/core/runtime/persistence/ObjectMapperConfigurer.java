package com.ecg.replyts.core.runtime.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class ObjectMapperConfigurer {

    public static ObjectMapper getObjectMapper() {
        return ObjectMapperHolder.INSTANCE;
    }

    public static ObjectNode objectBuilder() {
        return ObjectMapperConfigurer.getObjectMapper().createObjectNode();
    }

    public static ArrayNode arrayBuilder() {
        return ObjectMapperConfigurer.getObjectMapper().createArrayNode();
    }

    private static class ObjectMapperHolder {
        private static final ObjectMapper INSTANCE = new ObjectMapper()
                .registerModule(new JodaModule())
                .registerModule(new GuavaModule())
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}