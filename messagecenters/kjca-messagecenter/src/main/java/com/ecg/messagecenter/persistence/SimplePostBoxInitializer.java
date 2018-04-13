package com.ecg.messagecenter.persistence;

import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static org.joda.time.DateTime.now;

/**
 * KJCA PostBox initializer which filters blocked conversations and uses TextAnonymizer to anonimize and truncate the
 * preview message. It also creates new ConversationThreads with a few optional values from the conversation's
 * 'customValues' map.
 */
@Component
public class SimplePostBoxInitializer extends AbstractSimplePostBoxInitializer<ConversationThread> {
    @Autowired
    private ConversationBlockRepository conversationBlockRepository;

    @Autowired
    private TextAnonymizer textAnonymizer;

    private MessagesResponseFactory messageResponseFactory;

    @PostConstruct
    private void createMessagesResponseFactory() {
        messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer(), textAnonymizer);
    }

    @Override
    public boolean filter(String email, Conversation conversation) {
        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversation.getId());

        return conversationBlock != null && (
          (email.equals(conversation.getBuyerId()) && conversationBlock.getBuyerBlockedSellerAt().isPresent()) ||
          (email.equals(conversation.getSellerId()) && conversationBlock.getSellerBlockedBuyerAt().isPresent()));
    }

    @Override
    public Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        return messageResponseFactory.latestMessage(email, conversation)
          .map(latest -> textAnonymizer.anonymizeText(conversation, MessageCenterUtils.truncateText(latest.getTextShortTrimmed(), maxChars)));
    }

    @Override
    public ConversationThread newConversationThread(String email, Conversation conversation, boolean newReplyArrived, Message lastMessage) {
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
          Optional.ofNullable(Iterables.getLast(conversation.getMessages()).getMessageDirection().name()));
    }
}
