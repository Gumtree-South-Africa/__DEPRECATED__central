package com.ecg.messagecenter.gtau.persistence;

import com.ecg.messagecenter.core.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.core.util.MessageCenterUtils;
import com.ecg.messagecenter.gtau.util.ConversationThreadEnricher;
import com.ecg.messagecenter.gtau.util.MessageType;
import com.ecg.messagecenter.gtau.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.joda.time.DateTime.now;

/**
 * GTAU PostBox initializer which filters nothing and uses MessageCenterUtils to truncate the preview message. It also
 * creates new ConversationThreads with a few optional values from the conversation's 'customValues' map
 */
@Component
public class SimplePostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {

    private final ConversationThreadEnricher conversationThreadEnricher;

    @Autowired
    public SimplePostBoxInitializer(ConversationThreadEnricher conversationThreadEnricher, SimplePostBoxRepository simplePostBoxRepository) {
        super(simplePostBoxRepository);
        this.conversationThreadEnricher = conversationThreadEnricher;
    }

    @Override
    protected boolean filter(String email, Conversation conversation) {
        return false;
    }

    @Override
    public Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        return MessagesResponseFactory.latestMessage(email, conversation)
                .map(message -> MessageCenterUtils.truncateText(message.getTextShortTrimmed(), maxChars));
    }

    @Override
    public ConversationThread newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage) {
        return conversationThreadEnricher.enrichOnWrite(new ConversationThread(
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
                        Optional.ofNullable(conversation.getSellerId()),
                        Optional.ofNullable(lastMessage.getMessageDirection().name()),
                        Optional.ofNullable(MessageType.getRobot(lastMessage)),
                        Optional.ofNullable(MessageType.getOffer(lastMessage)),
                        lastMessage.getAttachmentFilenames(),
                        Optional.ofNullable(lastMessage.getId()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()),
                conversation);
    }
}
