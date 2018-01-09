package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Iterables;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

@Component
public class PostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {
	private static final String POSTBOX_CUSTOM_HEADER_PREFIX = "MC-";

    @Value("${replyts.maxPreviewMessageCharacters:250}")
    private int maxChars;

    private MessagesResponseFactory messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());

    @Override
    protected boolean filter(String email, Conversation conversation) {
        return false;
    }

    @Override
    protected Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        return messageResponseFactory.latestMessage(email, conversation)
          .map(message -> MessageCenterUtils.truncateText(message.getTextShortTrimmed(), maxChars));
    }

    @Override
    protected ConversationThread newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage) {
        Map<String, String> customValues = conversation.getCustomValues().entrySet().stream()
          .filter(e -> e.getKey().toLowerCase().startsWith(POSTBOX_CUSTOM_HEADER_PREFIX.toLowerCase()))
          .collect(Collectors.toMap(e -> e.getKey().substring(POSTBOX_CUSTOM_HEADER_PREFIX.length()), e -> e.getValue()));

        Optional<ConversationRole> closeBy = Optional.empty();

        if (conversation.getState().equals(ConversationState.CLOSED)) {
            closeBy = Optional.of(conversation.isClosedBy(ConversationRole.Buyer) ? ConversationRole.Buyer : ConversationRole.Seller);
        }

        return new ConversationThread(
          conversation.getAdId(),
          conversation.getId(),
          conversation.getCreatedAt(),
          now(),
          conversation.getLastModifiedAt(),
          newReplyArrived,
          extractPreviewLastMessage(conversation, email),
          Optional.ofNullable(conversation.getCustomValues().get("buyer-name")),
          Optional.ofNullable(conversation.getCustomValues().get("seller-name")),
          Optional.ofNullable(conversation.getBuyerId()),
          Optional.ofNullable(Iterables.getLast(conversation.getMessages()).getMessageDirection().name()),
          Optional.ofNullable(conversation.getState()),
          closeBy,
          Optional.of(customValues));
    }
}