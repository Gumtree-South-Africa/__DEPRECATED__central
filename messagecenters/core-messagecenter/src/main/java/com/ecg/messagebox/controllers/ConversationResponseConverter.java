package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.MessageResponse;
import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagecenter.util.MessageCenterUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ecg.messagecenter.util.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;

public class ConversationResponseConverter {

    public static ConversationResponse toConversationResponseWithMessages(ConversationThread conversationThread) {
        List<MessageResponse> messageResponses = conversationThread.getMessages().stream()
                .map(ConversationResponseConverter::toMessageResponse).collect(Collectors.toList());

        if (messageResponses.size() > 0) {
            int otherParticipantNumUnread = conversationThread.getHighestOtherParticipantNumUnread();
            for (int i = messageResponses.size() - 1; i >= messageResponses.size() - otherParticipantNumUnread && i >= 0; i--) {
                messageResponses.get(i).setIsRead(false);
            }
        }
        return toConversationResponse(conversationThread, Optional.of(messageResponses));
    }

    public static ConversationResponse toConversationResponse(ConversationThread conversationThread) {
        return toConversationResponse(conversationThread, Optional.empty());
    }

    private static ConversationResponse toConversationResponse(ConversationThread conversation, Optional<List<MessageResponse>> messageResponsesOpt) {
        List<ParticipantResponse> participantResponses = conversation.getParticipants().stream()
                .map(ConversationResponseConverter::toParticipantResponse)
                .collect(Collectors.toList());

        String creationDateStr = conversation.getMetadata().getCreationDate()
                .map(MessageCenterUtils::toFormattedTimeISO8601ExplicitTimezoneOffset).orElse(null);

        MessageResponse messageResponse = null;
        if (conversation.getLatestMessage() != null) {
            messageResponse = toMessageResponse(conversation.getLatestMessage()).withIsRead(conversation.getHighestOtherParticipantNumUnread() == 0);
        }
        return new ConversationResponse(
                conversation.getId(),
                conversation.getAdId(),
                conversation.getVisibility().name().toLowerCase(),
                conversation.getMessageNotification().name().toLowerCase(),
                participantResponses,
                messageResponse,
                creationDateStr,
                conversation.getMetadata().getEmailSubject(),
                conversation.getMetadata().getTitle().orElse(null),
                conversation.getMetadata().getImageUrl(),
                conversation.getNumUnreadMessages(conversation.getUserId()),
                messageResponsesOpt);
    }

    private static MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId().toString(),
                message.getType().getValue(),
                message.getText(),
                message.getSenderUserId(),
                toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                message.getCustomData());
    }

    private static ParticipantResponse toParticipantResponse(Participant participant) {
        return new ParticipantResponse(
                participant.getUserId(),
                participant.getName(),
                participant.getEmail(),
                participant.getRole().getValue());
    }
}