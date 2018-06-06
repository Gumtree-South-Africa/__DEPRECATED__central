package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.resources.responses.ConversationResponse;
import com.ecg.messagebox.resources.responses.MessageResponse;
import com.ecg.messagebox.resources.responses.ParticipantResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ConversationResponseConverter {

    static ConversationResponse toConversationResponseWithMessages(ConversationThread conversationThread) {
        List<MessageResponse> messageResponses = conversationThread.getMessages().stream()
                .map(ConversationResponseConverter::toMessageResponse).collect(Collectors.toList());

        if (messageResponses.size() > 0) {
            int otherParticipantNumUnread = conversationThread.getHighestOtherParticipantNumUnread();
            for (int i = messageResponses.size() - 1; i >= messageResponses.size() - otherParticipantNumUnread && i >= 0; i--) {
                messageResponses.get(i).setIsRead(false);
            }
        }
        return toConversationResponse(conversationThread, messageResponses);
    }

    static ConversationResponse toConversationResponse(ConversationThread conversationThread) {
        return toConversationResponse(conversationThread, Collections.emptyList());
    }

    private static ConversationResponse toConversationResponse(ConversationThread conversation, List<MessageResponse> messageResponses) {
        List<ParticipantResponse> participantResponses = conversation.getParticipants().stream()
                .map(ConversationResponseConverter::toParticipantResponse)
                .collect(Collectors.toList());

        MessageResponse messageResponse = null;
        if (conversation.getLatestMessage() != null) {
            messageResponse = toMessageResponse(conversation.getLatestMessage()).withIsRead(conversation.getHighestOtherParticipantNumUnread() == 0);
        }
        return new ConversationResponse(
                conversation.getId(),
                conversation.getAdId(),
                conversation.getVisibility(),
                conversation.getMessageNotification(),
                participantResponses,
                messageResponse,
                conversation.getMetadata().getCreationDate().orElse(null),
                conversation.getMetadata().getEmailSubject(),
                conversation.getMetadata().getTitle().orElse(null),
                conversation.getMetadata().getImageUrl(),
                conversation.getNumUnreadMessages(conversation.getUserId()),
                messageResponses);
    }

    private static MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId().toString(),
                message.getType(),
                message.getText(),
                message.getSenderUserId(),
                message.getReceivedDate(),
                message.getCustomData(),
                message.getMetadata().getHeaders());
    }

    private static ParticipantResponse toParticipantResponse(Participant participant) {
        return new ParticipantResponse(
                participant.getUserId(),
                participant.getName(),
                participant.getEmail(),
                participant.getRole());
    }
}