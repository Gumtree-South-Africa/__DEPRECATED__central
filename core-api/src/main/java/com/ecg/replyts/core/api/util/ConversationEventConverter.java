package com.ecg.replyts.core.api.util;

import com.ecg.comaas.events.Conversation;
import org.springframework.util.StringUtils;

public class ConversationEventConverter {

    public ConversationEventConverter() {
        throw new AssertionError();
    }

    //Move to utility class
    public static Conversation.Participant createParticipant(String userId, String userName, String userEmail, Conversation.Participant.Role role, String emailSecret) {
        Conversation.Participant.Builder builder = Conversation.Participant.newBuilder();
        if (userId != null) {
            builder.setUserId(userId);
        }
        if (userEmail != null) {
            builder.setEmail(userEmail);
        }
        if (role != null) {
            builder.setRole(role);
        }
        if (!StringUtils.isEmpty(userName)) {
            builder.setName(userName);
        }
        if (emailSecret != null) {
            builder.setEmailSecret(emailSecret);
        }

        return builder.build();
    }
}
