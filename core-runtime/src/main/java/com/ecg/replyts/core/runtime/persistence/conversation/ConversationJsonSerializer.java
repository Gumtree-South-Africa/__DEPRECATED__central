package com.ecg.replyts.core.runtime.persistence.conversation;


import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import java.io.IOException;
import java.util.List;

class ConversationJsonSerializer {

    private final ObjectWriter jsonWriter;
    private final ObjectReader jsonReader;

    public ConversationJsonSerializer() {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JodaModuleWithLocalDateTimeZone());
        jsonMapper.registerModule(new GuavaModule());

        TypeReference<List<ConversationEvent>> conversationEventListTypeReference =
                new TypeReference<List<ConversationEvent>>() {
                };
        jsonWriter = jsonMapper.writerWithType(conversationEventListTypeReference);
        jsonReader = jsonMapper.reader(conversationEventListTypeReference);
    }

    public byte[] serialize(List<ConversationEvent> domainObject) throws JsonProcessingException {
        return jsonWriter.writeValueAsBytes(domainObject);
    }

    public List<ConversationEvent> deserialize(byte[] json) throws IOException {
        return jsonReader.readValue(json);
    }

}
