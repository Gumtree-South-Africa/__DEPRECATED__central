package com.ecg.messagecenter.gtuk.persistence;

import com.ecg.gumtree.replyts2.common.message.MessageCenterUtils;
import com.ecg.messagecenter.core.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
import com.ecg.messagecenter.gtuk.util.MessagesDiffer;
import com.ecg.messagecenter.gtuk.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.joda.time.DateTime.now;

/**
 * GTUK PostBox initializer which filters nothing and uses MessageCenterUtils to truncate the preview message.
 */
@Component
public class SimplePostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {
    private MessagesResponseFactory messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());

    @Autowired
    public SimplePostBoxInitializer(SimpleMessageCenterRepository postBoxRepository) {
        super(postBoxRepository);
    }

    @Override
    protected boolean filter(String email, Conversation conversation) {
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
                Optional.ofNullable(conversation.getSellerId()),
                Optional.ofNullable(lastMessage.getMessageDirection().name()));
    }
}