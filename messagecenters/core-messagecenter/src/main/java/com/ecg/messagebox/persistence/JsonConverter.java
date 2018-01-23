package com.ecg.messagebox.persistence;

import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageMetadata;
import com.ecg.messagebox.model.Participant;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class JsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverter.class);

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperConfigurer.getObjectMapper();

    public static String toParticipantsJson(String userId, String conversationId, List<Participant> participants) {
        try {
            return OBJECT_MAPPER.writeValueAsString(participants);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize participants for userId {} and conversationId {}: {}",
                    userId, conversationId, participants, e);
            throw new RuntimeException(e);
        }
    }

    public static List<Participant> fromParticipantsJson(String userId, String conversationId, String jsonValue) {
        try {
            return OBJECT_MAPPER.readValue(jsonValue,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Participant.class));
        } catch (IOException e) {
            LOGGER.error("Could not deserialize participants of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public static String toMessageJson(String userId, String conversationId, Message message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize message for userId {} and conversationId {}: {}",
                    userId, conversationId, message, e);
            throw new RuntimeException(e);
        }
    }

    public static Message fromMessageJson(String userId, String conversationId, String jsonValue) {
        try {
            return OBJECT_MAPPER.readValue(jsonValue, Message.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize latest message of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public static String toMessageMetadataJson(String userId, String conversationId, Message message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message.getMetadata());
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize message metadata for userId {}, conversationId {} and messageId {}: {}",
                    userId, conversationId, message.getId(), message.getMetadata(), e);
            throw new RuntimeException(e);
        }
    }

    public static MessageMetadata fromMessageMetadataJson(String userId, String conversationId, String messageId, String jsonValue) {
        try {
            return OBJECT_MAPPER.readValue(jsonValue, MessageMetadata.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize message metadata of userId {}, conversationId {} and messageId {}, json: {}",
                    userId, conversationId, messageId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }

    public static String toConversationMetadataJson(String userId, String conversationId, ConversationMetadata metadata) {
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialize conversation metadata for userId {} and conversationId {}: {}",
                    userId, conversationId, metadata, e);
            throw new RuntimeException(e);
        }
    }

    public static ConversationMetadata fromConversationMetadataJson(String userId, String conversationId, String jsonValue) {
        try {
            return OBJECT_MAPPER.readValue(jsonValue, ConversationMetadata.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize conversation metadata of userId {} and conversationId {}, json: {}",
                    userId, conversationId, jsonValue, e);
            throw new RuntimeException(e);
        }
    }
}