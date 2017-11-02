package com.ecg.messagecenter.diff;

import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.ecg.messagecenter.util.TextDiffer;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

class ConversationRoleUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationRoleUtil.class);

    static ConversationRole getConversationRole(String projectionOwnerUserId, List<Participant> conversationParticipants) {
        Optional<Participant> projectionOwnerParticipant = conversationParticipants.stream()
                .filter(p -> p.getUserId().equals(projectionOwnerUserId))
                .findFirst();

        if (projectionOwnerParticipant.isPresent()) {
            return projectionOwnerParticipant.get().getRole() == ParticipantRole.BUYER ? ConversationRole.Buyer : ConversationRole.Seller;
        } else {
            LOG.warn("Current user was not found among the participants.");
            return null;
        }
    }

    static ConversationRole getConversationRole(String userId, String buyerId, String sellerId) {
        if (userId.equals(buyerId)) {
            return ConversationRole.Buyer;
        } else if (userId.equals(sellerId)) {
            return ConversationRole.Seller;
        } else {
            LOG.warn("Current user was not found among the participants.");
            return null;
        }
    }
}