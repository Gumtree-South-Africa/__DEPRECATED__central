package com.ecg.replyts.core.runtime.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class JacksonAwareObjectMapperConfigurer {

    private final ObjectMapper objectMapper;

    public JacksonAwareObjectMapperConfigurer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JodaModule())
                .registerModule(new GuavaModule())
                .registerModule(new Jdk8Module());
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}