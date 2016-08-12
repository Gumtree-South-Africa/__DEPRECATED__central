package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.ConversationThread;
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

        return new ConversationResponse(
                conversation.getId(),
                conversation.getAdId(),
                conversation.getVisibility().name().toLowerCase(),
                conversation.getMessageNotification().name().toLowerCase(),
                participantResponses,
                msgRespConverter.toMessageResponse(conversation.getLatestMessage()),
                conversation.getMetadata().getEmailSubject(),
                conversation.getNumUnreadMessages(),
                messageResponsesOpt);
    }
}