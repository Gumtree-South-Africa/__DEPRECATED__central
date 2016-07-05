package com.ecg.replyts.core.api.model.conversation.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageAddedEventTest {
    @Test
    public void canParseJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());

        MessageFilteredEvent messageFilteredEvent = objectMapper
                .reader(MessageFilteredEvent.class)
                .readValue(getClass().getResourceAsStream("message_filtered_event.json"));

        assertEquals("MessageFilteredEvent-3:aqn58e:ihfxk67p-1448523755385", messageFilteredEvent.getEventId());
    }
}