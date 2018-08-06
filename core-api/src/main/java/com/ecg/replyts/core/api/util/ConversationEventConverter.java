package com.ecg.replyts.core.api.util;

import com.ecg.comaas.events.Conversation;
import org.springframework.util.StringUtils;

public class ConversationEventConverter {

    public ConversationEventConverter() {
        throw new AssertionError();
    }

    //Move to utility class
    public static Conversation.Participant createParticipant(String userId, String userName, String userEmail, Conversation.Participant.Role role) {
        Conversation.Participant.Builder builder = Conversation.Participant.newBuilder()
                .setUserId(userId)
                .setEmail(userEmail)
                .setRole(role);

        if (!StringUtils.isEmpty(userName)) {
            builder.setName(userName);
        }

        return builder.build();
    }
}
