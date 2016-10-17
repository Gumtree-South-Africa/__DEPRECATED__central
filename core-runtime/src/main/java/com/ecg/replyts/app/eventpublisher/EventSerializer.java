package com.ecg.replyts.app.eventpublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Serializes events to JSON.
 */
public class EventSerializer {

    private final ObjectWriter objectWriter;

    public EventSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectWriter = objectMapper.writer();
    }

    public <T> byte[] serialize(T extendedConversationEvent) {
        try {
            return objectWriter.writeValueAsBytes(extendedConversationEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize event " + extendedConversationEvent, e);
        }
    }
}
