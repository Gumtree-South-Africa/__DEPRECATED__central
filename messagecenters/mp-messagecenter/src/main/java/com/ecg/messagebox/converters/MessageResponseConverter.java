package com.ecg.messagebox.converters;

import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageResponseConverter {

    public MessageResponse toMessageResponse(Message message, String projectionOwnerUserId, List<Participant> conversationParticipants) {
        Participant participant1 = conversationParticipants.get(0);
        Participant participant2 = conversationParticipants.get(1);
        String senderEmail = participant1.getUserId().equals(message.getSenderUserId()) ?
                participant1.getEmail() : participant2.getEmail();

        MailTypeRts boundness = message.getSenderUserId().equals(projectionOwnerUserId) ? MailTypeRts.OUTBOUND : MailTypeRts.INBOUND;

        return new MessageResponse(
                MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                boundness,
                message.getText(),
                senderEmail);
    }
}