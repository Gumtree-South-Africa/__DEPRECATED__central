package com.ecg.replyts.core.api.util;

import com.ecg.comaas.events.Conversation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConversationEventConverterTest {

    private static final String USER_ID = "userId";
    private static final String SECRET = "secret";

    @Test
    public void createParticipantWithUserIdOnly() {
        Conversation.Participant participant = ConversationEventConverter.createParticipant(USER_ID, null, null, null, null);

        assertEquals(USER_ID, participant.getUserId());
    }

    @Test
    public void createParticipantWithSecret() {
        Conversation.Participant participant = ConversationEventConverter.createParticipant(USER_ID, null, null, null, SECRET);

        assertEquals(SECRET, participant.getEmailSecret());
    }
}