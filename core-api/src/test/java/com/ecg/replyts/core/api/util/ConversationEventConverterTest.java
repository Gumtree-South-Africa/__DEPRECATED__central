package com.ecg.replyts.core.api.util;

import com.ecg.comaas.events.Conversation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConversationEventConverterTest {

    private static final String USER_ID = "userId";
    private static final String ANONYMIZED_EMAIL = "anonymized@mail.com";

    @Test
    public void createParticipantWithUserIdOnly() {
        Conversation.Participant participant = ConversationEventConverter.createParticipant(USER_ID, null, null, null, null);

        assertEquals(USER_ID, participant.getUserId());
    }

    @Test
    public void createParticipantWithSecret() {
        Conversation.Participant participant = ConversationEventConverter.createParticipant(USER_ID, null, null, null, ANONYMIZED_EMAIL);

        assertEquals(ANONYMIZED_EMAIL, participant.getCloakedEmailAddress());
    }
}