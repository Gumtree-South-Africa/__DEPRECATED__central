package com.ecg.de.ebayk.messagecenter.util;

import com.ecg.de.ebayk.messagecenter.persistence.Header;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageTypeTest {
    private Message message;

    @Before
    public void setup() {
        message = mock(Message.class);
    }

    @Test
    public void testHasLinks() throws Exception {
        when(message.getHeaders()).thenReturn(headers());
        assertTrue("Message should contain links", MessageType.hasLinks(message));
    }

    @Test
    public void testGetLinks() throws Exception {
        when(message.getHeaders()).thenReturn(headers());
        assertNotNull(MessageType.getLinks(message));
        assertEquals(1, MessageType.getLinks(message).size());
    }

    private Map<String,String> headers() {
        Map<String,String> headers = Maps.newHashMap();
        headers.put(Header.MessageLinks.getValue(), "[{\"end\":68,\"start\":64,\"type\":\"SEARCH\",\"url\":\"/s-motorcycles/city-canberra/c18626l3003021?ad=offering\"}]");
        return headers;
    }
}