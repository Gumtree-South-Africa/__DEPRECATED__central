package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessageType;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.joda.time.DateTime.now;

/**
 * IT PostBox initializer which filters non-SENT messages and uses MessageCenterUtils to truncate the preview message.
 */
@Component
public class SimplePostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {
    private static final Logger LOG = LoggerFactory.getLogger(SimplePostBoxInitializer.class);

    private MessagesResponseFactory messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());

    @Override
    protected boolean filter(String email, Conversation conversation) {
        Message lastMessage = Iterables.getLast(conversation.getMessages());

        logMessage(lastMessage);

        if (lastMessage.getState() != MessageState.SENT) {
            LOG.info("Ignoring message " + lastMessage);

            return true;
        }

        return false;
    }

    @Override
    public Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        return messageResponseFactory.latestMessage(email, conversation)
          .map(message -> MessageCenterUtils.truncateText(message.getTextShortTrimmed(), maxChars));
    }

    @Override
    public ConversationThread newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage) {
        return new ConversationThread(
          conversation.getAdId(),
          conversation.getId(),
          conversation.getCreatedAt(),
          now(),
          lastMessage.getReceivedAt(),
          newReplyArrived,
          extractPreviewLastMessage(conversation, email),
          Optional.ofNullable(conversation.getCustomValues().get("buyer-name")),
          Optional.ofNullable(conversation.getCustomValues().get("seller-name")),
          Optional.ofNullable(conversation.getBuyerId()),
          Optional.ofNullable(lastMessage.getMessageDirection().name()),
          Optional.ofNullable(MessageType.getRobot(lastMessage)),
          Optional.ofNullable(MessageType.getOffer(lastMessage)));
    }

    private void logMessage(Message message) {
        try {
            LOG.debug("Message: " + new ObjectMapper().writeValueAsString(message));
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
    }
}