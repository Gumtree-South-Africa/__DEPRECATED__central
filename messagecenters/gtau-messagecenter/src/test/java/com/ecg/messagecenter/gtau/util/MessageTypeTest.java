package com.ecg.messagecenter.gtau.util;

import com.ecg.messagecenter.gtau.persistence.Header;
import com.ecg.messagecenter.gtau.util.MessageType;
import com.ecg.messagecenter.gtau.webapi.responses.MessageResponse;
import com.ecg.messagecenter.gtau.webapi.responses.RobotMessageResponse;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    @Test
    public void testGetRobotDetailsNonRobotReturnsEmpty() throws Exception {
        Map<String, String> headers = new HashMap<>();
        when(message.getHeaders()).thenReturn(headers);

        assertFalse("Robot Details must be empty.", MessageType.getRobotDetails(message).isPresent());
    }

    @Test
    public void testGetRobotDetailsWithEmptySenderReturnsEmpty() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Header.Robot.getValue(), "GTAU");
        when(message.getHeaders()).thenReturn(headers);

        assertFalse("Robot Details must be empty.", MessageType.getRobotDetails(message).isPresent());

    }

    @Test
    public void testGetRobotDetailsWithNameButNoIconsReturnsCorrectDetails() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Header.Robot.getValue(), "GTAU");
        headers.put(Header.MessageSender.getValue(), "{\"name\":\"Gumbot\"}");

        when(message.getHeaders()).thenReturn(headers);

        Optional<RobotMessageResponse> response = MessageType.getRobotDetails(message);
        assertTrue("Robot Details must be defined.", response.isPresent());

        assertEquals("Gumbot", response.get().getSender().getName());
        assertTrue("No icons should be defined.", response.get().getSender().getSenderIcon().isEmpty());
        assertNull("No rich message must be defined.", response.get().getRichMessage());
    }

    @Test
    public void testGetRobotDetailsWithSenderAndIconsMustReturnCorrectDetails() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Header.Robot.getValue(), "GTAU");
        headers.put(Header.MessageSender.getValue(), "{\"name\":\"Another Gumbot\",\"senderIcons\":[{\"name\":\"test\",\"url\":\"http://test\"},{\"name\":\"test2\",\"url\":\"http://test2\"}]}");

        when(message.getHeaders()).thenReturn(headers);

        Optional<RobotMessageResponse> response = MessageType.getRobotDetails(message);
        assertTrue("Robot Details must be defined.", response.isPresent());

        assertEquals("Another Gumbot", response.get().getSender().getName());
        assertEquals(2, response.get().getSender().getSenderIcon().size());
        assertEquals(2, response.get().getSender().getSenderIcon().size());
        assertEquals("test", response.get().getSender().getSenderIcon().get(0).getName());
        assertEquals("http://test", response.get().getSender().getSenderIcon().get(0).getUrl());
        assertEquals("test2", response.get().getSender().getSenderIcon().get(1).getName());
        assertEquals("http://test2", response.get().getSender().getSenderIcon().get(1).getUrl());

        assertNull("No rich message must be defined.", response.get().getRichMessage());
    }

    @Test
    public void testGetRobotDetailsWithRichMessageAndLinksReturnsCorrectDetails() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Header.Robot.getValue(), "GTAU");
        headers.put(Header.MessageSender.getValue(), "{\"name\":\"Gumbot\"}");
        headers.put(Header.RichTextLinks.getValue(), "[{\"end\":68,\"start\":64,\"type\":\"SEARCH\",\"url\":\"/s-motorcycles/city-canberra/c18626l3003021?ad=offering\"}]");
        headers.put(Header.RichTextMessage.getValue(), "<div>Testing this is a Rich Message with <a>a link!</a></div>");

        when(message.getHeaders()).thenReturn(headers);

        Optional<RobotMessageResponse> response = MessageType.getRobotDetails(message);
        assertTrue("Robot Details must be defined.", response.isPresent());

        assertEquals("Gumbot", response.get().getSender().getName());
        assertTrue("Sender icons must be null.", response.get().getSender().getSenderIcon().isEmpty());
        assertEquals("<div>Testing this is a Rich Message with <a>a link!</a></div>", response.get().getRichMessage().getRichMessage());
        assertEquals(1, response.get().getRichMessage().getMessageLinks().size());

        MessageResponse.MessageLink link = response.get().getRichMessage().getMessageLinks().get(0);
        assertEquals("SEARCH", link.getType());
        assertEquals("/s-motorcycles/city-canberra/c18626l3003021?ad=offering", link.getUrl());
        assertEquals(68, link.getEnd());
        assertEquals(64, link.getStart());
    }


    private Map<String,String> headers() {
        Map<String,String> headers = Maps.newHashMap();
        headers.put(Header.MessageLinks.getValue(), "[{\"end\":68,\"start\":64,\"type\":\"SEARCH\",\"url\":\"/s-motorcycles/city-canberra/c18626l3003021?ad=offering\"}]");
        return headers;
    }
}
