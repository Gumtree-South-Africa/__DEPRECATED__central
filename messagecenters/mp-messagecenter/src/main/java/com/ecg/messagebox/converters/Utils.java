package com.ecg.messagebox.converters;

import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;

import java.util.List;

class Utils {

    static ConversationRole getConversationRole(String projectionOwnerUserId, List<Participant> conversationParticipants) {
        Participant projectionOwnerParticipant = conversationParticipants.stream()
                .filter(p -> p.getUserId().equals(projectionOwnerUserId))
                .findFirst().get();
        return projectionOwnerParticipant.getRole() == ParticipantRole.BUYER ? ConversationRole.Buyer : ConversationRole.Seller;
    }
}