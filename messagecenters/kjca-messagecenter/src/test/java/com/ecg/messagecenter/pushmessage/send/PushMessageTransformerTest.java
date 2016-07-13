package com.ecg.messagecenter.pushmessage.send;

import com.ecg.messagecenter.pushmessage.PushMessagePayload;
import com.ecg.messagecenter.pushmessage.send.client.SendClient;
import com.ecg.messagecenter.pushmessage.send.model.SendMessage;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PushMessageTransformerTest {
    private PushMessageTransformer transformer;

    @Before
    public void setUp() throws Exception {
        transformer = new PushMessageTransformer();
    }

    @Test
    public void transformPushRequestToSendMessage() throws Exception {
        PushMessagePayload payload = new PushMessagePayload("a", "1234", "b", "c", Optional.of(ImmutableMap.of("a1", "1", "ConversationId", "e")), Optional.of(10));

        SendMessage sendMessage = transformer.from(payload);

        assertNull("There shouldn't be any id at this stage.", sendMessage.getId());
        assertEquals("user id should be at the right place", Long.valueOf(1234), sendMessage.getUserId());
        assertEquals("message should appear", "b", sendMessage.getMessage());
        assertEquals("alert counter should appear", Integer.valueOf(10), sendMessage.getAlertCounter());
        assertEquals("notification type should always be CHATMESSAGE", SendClient.NotificationType.CHATMESSAGE, sendMessage.getType());
        assertEquals("reference id should be conversation id", "e", sendMessage.getReferenceId());
        assertTrue(sendMessage.getDetails().size() > 0);
    }
}
