package com.ecg.replyts.core.api.model.conversation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExtendedConversationEventTest {

    @Test
    public void canParseJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        ExtendedConversationEvent conversationEvent = objectMapper
                .reader(ExtendedConversationEvent.class)
                .readValue(getClass().getResourceAsStream("extended_conversation_event.json"));

        assertEquals("MessageFilteredEvent-3:aqn58e:ihfxk67p-1448523755385", conversationEvent.event.getEventId());
        assertEquals("2:aqn58e:ihfxk67p", conversationEvent.conversation.conversationId);
    }

}
