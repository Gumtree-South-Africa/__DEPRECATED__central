package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConversationJsonSerializerTest {

    private ConversationJsonSerializer serializer = new ConversationJsonSerializer();
    private final DateTime createdDate = DateTime.parse("2010-01-01T00:00:00.000+01:00");
    private ConversationCreatedEvent conversationCreatedEvent  = new ConversationCreatedEvent("convId", "adId", "buyerId", "sellerId", "buyerSecret", "sellerSecret", createdDate, ConversationState.ACTIVE, Collections.<String, String>emptyMap());;

    @Test
    public void generatesValidJson() throws JsonProcessingException {
        byte[] serialize = serializer.serialize(Arrays.<ConversationEvent>asList(conversationCreatedEvent));

        JsonObjects.parse(new String(serialize));
    }

    @Test
    public void generatesArrayOfJsonObjects() throws JsonProcessingException {
        byte[] serialize = serializer.serialize(Arrays.<ConversationEvent>asList(conversationCreatedEvent));

        JsonNode parse = JsonObjects.parse(new String(serialize));
        assertTrue(parse instanceof ArrayNode);

    }

    @Test
    public void serializesDateFormat() throws JsonProcessingException {
        byte[] serialize = serializer.serialize(Arrays.<ConversationEvent>asList(conversationCreatedEvent));

        JsonNode parse = JsonObjects.parse(new String(serialize)).get(0);

        assertEquals(createdDate.getMillis()+"", parse.get("createdAt").asText());
    }

    @Test
    public void loadsEventFromJson() throws IOException {
        List<ConversationEvent> resolved = serializer.deserialize(serializer.serialize(Arrays.<ConversationEvent>asList(conversationCreatedEvent)));


        assertEquals(conversationCreatedEvent, resolved.get(0));
    }
}
