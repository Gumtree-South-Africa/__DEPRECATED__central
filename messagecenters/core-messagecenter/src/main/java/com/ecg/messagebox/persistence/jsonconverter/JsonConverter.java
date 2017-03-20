package com.ecg.messagebox.persistence.jsonconverter;

import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageMetadata;
import com.ecg.messagebox.model.Participant;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);

    private ObjectMapper objectMapper;

    @Autowired
    public JsonConverter(JacksonAwareObjectMapperConfigurer configurer) {
        objectMapper = configurer.getObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String toParticipantsJson(String userId, String conversationId, List<Participant> participants) {
        String participantsJson;
        try {
            participantsJson = objectMapper.writeValueAsString(participants);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize participants for userId {} and conversationId {}: {}",
                    userId, conversationId, participants, e);
            throw new RuntimeException(e);
        }
        return participantsJson;
    }

    public List<Participant> fromParticipantsJson(String userId, String conversationId, String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Participant.class));
        } catch (IOException e) {
            LOGGER.error("Could not deserialize participants of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public String toMessageJson(String userId, String conversationId, Message message) {
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize message for userId {} and conversationId {}: {}",
                    userId, conversationId, message, e);
            throw new RuntimeException(e);
        }
        return messageJson;
    }

    public Message fromMessageJson(String userId, String conversationId, String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue, Message.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize latest message of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public String toMessageMetadataJson(String userId, String conversationId, Message message) {
        String messageMetadataJson;
        try {
            messageMetadataJson = objectMapper.writeValueAsString(message.getMetadata());
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize message metadata for userId {}, conversationId {} and messageId {}: {}",
                    userId, conversationId, message.getId(), message.getMetadata(), e);
            throw new RuntimeException(e);
        }
        return messageMetadataJson;
    }

    public MessageMetadata fromMessageMetadataJson(String userId, String conversationId, String messageId, String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue, MessageMetadata.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize message metadata of userId {}, conversationId {} and messageId {}, json: {}",
                    userId, conversationId, messageId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public String toConversationMetadataJson(String userId, String conversationId, ConversationMetadata metadata) {
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize conversation metadata for userId {} and conversationId {}: {}",
                    userId, conversationId, metadata, e);
            throw new RuntimeException(e);
        }
        return metadataJson;
    }

    public ConversationMetadata fromConversationMetadataJson(String userId, String conversationId, String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue, ConversationMetadata.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize conversation metadata of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }
}