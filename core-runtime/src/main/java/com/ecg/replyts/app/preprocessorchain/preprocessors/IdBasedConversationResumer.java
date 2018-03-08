package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(value = "conversation.resume.strategy", havingValue = "id")
public class IdBasedConversationResumer extends ConversationResumer {

    @Autowired
    private UserIdentifierService userIdentifierService;

    @Override
    public boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context) {
        Mail mail = context.getMail().get();

        Map<String, String> headers = mail.getCustomHeaders();
        String buyerAddress = Optional.ofNullable(mail.getReplyTo()).orElse(mail.getFrom());
        String buyerId = userIdentifierService.getBuyerUserId(headers).orElse(buyerAddress);
        String sellerId = userIdentifierService.getSellerUserId(headers).orElse(mail.getDeliveredTo());

        ConversationIndexKey indexKeyBuyerToSeller = new ConversationIndexKey(buyerId, sellerId, mail.getAdId());
        ConversationIndexKey indexKeySellerToBuyer = new ConversationIndexKey(sellerId, buyerId, mail.getAdId());

        return tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSeller)
            || tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyer);
    }

    @Override
    public ConversationIndexKey keyFromCreatedEvent(ConversationCreatedEvent event) {
        return new ConversationIndexKey(
          userIdentifierService.getBuyerUserId(event.getCustomValues()).orElse(event.getBuyerId()),
          userIdentifierService.getSellerUserId(event.getCustomValues()).orElse(event.getSellerId()),
          event.getAdId());
    }

    @Override
    public ConversationIndexKey keyFromConversation(Conversation conversation) {
        return new ConversationIndexKey(
          userIdentifierService.getBuyerUserId(conversation).orElse(conversation.getBuyerId()),
          userIdentifierService.getSellerUserId(conversation).orElse(conversation.getSellerId()),
          conversation.getAdId());
    }
}