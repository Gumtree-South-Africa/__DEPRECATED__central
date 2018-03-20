package com.ecg.messagecenter.diff;

import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;

class ConversationDiffUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationDiffUtil.class);

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

    static MessageResponse toMessageResponse(Message message, String projectionOwnerUserId, List<Participant> participants) {
        Participant participant1 = participants.get(0);
        Participant participant2 = participants.get(1);
        String senderEmail = participant1.getUserId().equals(message.getSenderUserId()) ?
                participant1.getEmail() : participant2.getEmail();

        MailTypeRts boundness = message.getSenderUserId().equals(projectionOwnerUserId) ? MailTypeRts.OUTBOUND : MailTypeRts.INBOUND;

        return new MessageResponse(
                toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                Optional.empty(),
                Optional.empty(),
                boundness,
                message.getText(),
                Optional.empty(),
                Collections.emptyList(),
                senderEmail,
                Collections.emptyList(),
                Optional.empty());
    }
}
