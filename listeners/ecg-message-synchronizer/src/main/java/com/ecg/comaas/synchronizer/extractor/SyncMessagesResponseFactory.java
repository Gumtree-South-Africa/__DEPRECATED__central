package com.ecg.comaas.synchronizer.extractor;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Copied class from MessageBox {@link com.ecg.messagebox.util.MessagesResponseFactory}
 */
@Component
public class SyncMessagesResponseFactory {

    @Autowired
    private SyncMessagePreProcessor messagePreProcessor;

    public String getCleanedMessage(Conversation conv, Message message) {
        if (messageBodyMarkedByNonPrintableCharacters(message)) {
            return MessageBodyExtractor.extractBodyMarkedByNonPrintableChars(message.getPlainTextBody());
        } else if (contactPosterForExistingConversation(message) || comesFromMessageBoxClient(message)) {
            return getUserMessage(message);
        } else {
            return messagePreProcessor.removeEmailClientReplyFragment(conv, message);
        }
    }

    private String getUserMessage(Message message) {
        String userMessageFromHeader;
        String userMessage;
        if ((userMessageFromHeader = message.getHeaders().get("X-Contact-Poster-User-Message")) != null) {
            userMessage = EmailHeaderFolder.unfold(userMessageFromHeader);
        } else if ((userMessageFromHeader = message.getHeaders().get("X-User-Message")) != null) {
            userMessage = EmailHeaderFolder.unfold(userMessageFromHeader);
        } else {
            userMessage = message.getPlainTextBody();
        }
        return userMessage.trim();
    }

    private boolean comesFromMessageBoxClient(Message messageRts) {
        return messageRts.getHeaders().containsKey("X-Reply-Channel") &&
                (messageRts.getHeaders().get("X-Reply-Channel").contains("api_") ||
                        messageRts.getHeaders().get("X-Reply-Channel").contains("desktop"));
    }

    private boolean contactPosterForExistingConversation(Message messageRts) {
        return messageRts.getHeaders().containsKey("X-Reply-Channel") &&
                messageRts.getHeaders().get("X-Reply-Channel").startsWith("cp_");
    }

    private boolean messageBodyMarkedByNonPrintableCharacters(Message messageRts) {
        return messageRts.getHeaders().containsKey("X-Cust-Msg-Body-Mark") &&
                messageRts.getHeaders().get("X-Cust-Msg-Body-Mark").contains("non-printable-chars");
    }
}