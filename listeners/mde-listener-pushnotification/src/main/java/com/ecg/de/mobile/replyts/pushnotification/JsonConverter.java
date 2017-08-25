package com.ecg.de.mobile.replyts.pushnotification;

import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonConverter {
    private ObjectMapper objectMapper;

    @Autowired
    public JsonConverter(@Qualifier("jacksonAwareObjectMapperConfigurer") JacksonAwareObjectMapperConfigurer configurer) {
        this.objectMapper = configurer.getObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public <T> T toObject(String jsonValue, Class<T> clazz) throws IOException {
        return objectMapper.readValue(jsonValue, clazz);
    }

    public String toJsonString(Object input) throws JsonProcessingException {
        return objectMapper.writeValueAsString(input);
    }
}