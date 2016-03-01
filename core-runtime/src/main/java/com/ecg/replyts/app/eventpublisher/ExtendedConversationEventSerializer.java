package com.ecg.replyts.app.eventpublisher;

import com.ecg.replyts.core.api.model.conversation.event.ExtendedConversationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Serializes instances of {@link ExtendedConversationEvent} to JSON.
 */
public class ExtendedConversationEventSerializer {

    private final ObjectWriter objectWriter;

    public ExtendedConversationEventSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectWriter = objectMapper.writer();
    }

    public byte[] serialize(ExtendedConversationEvent extendedConversationEvent) {
        try {
            return objectWriter.writeValueAsBytes(extendedConversationEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize extendedConversationEvent " + extendedConversationEvent, e);
        }
    }
}
