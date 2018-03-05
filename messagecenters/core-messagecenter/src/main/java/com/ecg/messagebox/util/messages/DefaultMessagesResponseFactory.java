package com.ecg.messagebox.util.messages;

import com.ecg.messagebox.util.MessagePreProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ecg.messagebox.util.EmailHeaderFolder.unfold;

@Component
public class DefaultMessagesResponseFactory implements MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMessagesResponseFactory.class);

    /**
     * Invisible separator used as marker for text.
     * See http://www.fileformat.info/info/unicode/char/2063/index.htm for more details.
     */
    static final String MARKER = "\u2063";
    private static final Pattern pattern = Pattern.compile(MARKER + "(.*)" + MARKER, Pattern.DOTALL);

    @Autowired
    private MessagePreProcessor messagePreProcessor;

    public String getCleanedMessage(Conversation conv, Message message) {
        if (messageBodyMarkedByNonPrintableCharacters(message)) {
            return extractBodyMarkedByNonPrintableChars(message.getPlainTextBody());
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
            userMessage = unfold(userMessageFromHeader);
        } else if ((userMessageFromHeader = message.getHeaders().get("X-User-Message")) != null) {
            userMessage = unfold(userMessageFromHeader);
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

    static String extractBodyMarkedByNonPrintableChars(String message) {
        try {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            LOG.error("failed to extract text body using non printable characters: {}", e.getMessage(), e);
        }

        return message;
    }
}