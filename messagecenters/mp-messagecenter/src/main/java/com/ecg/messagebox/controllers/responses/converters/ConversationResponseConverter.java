package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagecenter.util.MessageCenterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ConversationResponseConverter {

    private final ParticipantResponseConverter participantRespConverter;
    private final MessageResponseConverter msgRespConverter;

    @Autowired
    public ConversationResponseConverter(ParticipantResponseConverter participantRespConverter,
                                         MessageResponseConverter msgRespConverter) {
        this.participantRespConverter = participantRespConverter;
        this.msgRespConverter = msgRespConverter;
    }

    public ConversationResponse toConversationResponseWithMessages(ConversationThread conversationThread) {
        List<MessageResponse> messageResponses = conversationThread.getMessages().stream()
                .map(msgRespConverter::toMessageResponse).collect(Collectors.toList());

        int otherParticipantUnreadMessages = conversationThread.getHighestOtherParticipantUnreadMessages();
        for (int i = messageResponses.size() - otherParticipantUnreadMessages; i < messageResponses.size(); i++) {
            messageResponses.get(i).withIsRead(false);
        }
        return toConversationResponse(conversationThread, Optional.of(messageResponses));
    }

    public ConversationResponse toConversationResponse(ConversationThread conversationThread) {
        return toConversationResponse(conversationThread, Optional.empty());
    }

    private ConversationResponse toConversationResponse(ConversationThread conversation,
                                                        Optional<List<MessageResponse>> messageResponsesOpt) {
        List<ParticipantResponse> participantResponses = conversation.getParticipants().stream()
                .map(participantRespConverter::toParticipantResponse)
                .collect(Collectors.toList());

        String creationDateStr = conversation.getMetadata().getCreationDate()
                .map(MessageCenterUtils::toFormattedTimeISO8601ExplicitTimezoneOffset).orElse(null);

        return new ConversationResponse(
                conversation.getId(),
                conversation.getAdId(),
                conversation.getVisibility().name().toLowerCase(),
                conversation.getMessageNotification().name().toLowerCase(),
                participantResponses,
                msgRespConverter.toMessageResponse(conversation.getLatestMessage()).withIsRead(conversation.getHighestOtherParticipantUnreadMessages() == 0),
                creationDateStr,
                conversation.getMetadata().getEmailSubject(),
                conversation.getNumUnreadMessages(conversation.getUserId()),
                messageResponsesOpt);
    }
}