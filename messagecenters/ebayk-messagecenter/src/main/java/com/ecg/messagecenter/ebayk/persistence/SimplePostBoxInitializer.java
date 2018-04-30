package com.ecg.messagecenter.ebayk.persistence;

import com.ecg.messagecenter.core.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.messagecenter.ebayk.util.MessagesDiffer;
import com.ecg.messagecenter.ebayk.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.joda.time.DateTime.now;

/**
 * eBayK PostBox initializer which filters nothing and uses MessageCenterUtils to truncate the preview message. It also
 * creates new ConversationThreads with a few optional values from the conversation's 'customValues' map.
 */
@Component
public class SimplePostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {
    private MessagesResponseFactory messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());

    @Autowired
    public SimplePostBoxInitializer(SimplePostBoxRepository postBoxRepository) {
        super(postBoxRepository);
    }

    @Override
    protected boolean filter(String email, Conversation conversation) {
        return false;
    }

    @Override
    protected Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        return messageResponseFactory.latestMessage(email, conversation)
          .map(message -> MessageCenterUtils.truncateText(message.getTextShortTrimmed(), 250));
    }

    @Override
    protected ConversationThread newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage) {
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
          customValueAsLong(conversation, "user-id-buyer"),
          customValueAsLong(conversation, "user-id-seller"));
    }

    private Optional<Long> customValueAsLong(Conversation conversation, String customValueKey) {
        return Optional.ofNullable(conversation.getCustomValues().get(customValueKey))
          .map(value -> Long.parseLong(value));
    }
}
