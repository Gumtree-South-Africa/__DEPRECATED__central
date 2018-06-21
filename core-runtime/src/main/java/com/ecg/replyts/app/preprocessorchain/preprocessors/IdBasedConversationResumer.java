package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(value = "conversation.resume.strategy", havingValue = "id")
public class IdBasedConversationResumer extends ConversationResumer {
    private static final Logger LOG = LoggerFactory.getLogger(IdBasedConversationResumer.class);

    @Autowired
    private UserIdentifierService userIdentifierService;

    @Override
    public boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context) {
        Mail mail = context.getMail().get();

        Map<String, String> headers = mail.getCustomHeaders();
        String buyerAddress = Optional.ofNullable(mail.getReplyTo()).orElse(mail.getFrom());
        String sellerAddress = mail.getDeliveredTo();

        String buyerId = userIdentifierService.getBuyerUserId(headers).orElse(buyerAddress);
        String sellerId = userIdentifierService.getSellerUserId(headers).orElse(sellerAddress);

        ConversationIndexKey indexKeyBuyerToSeller = new ConversationIndexKey(buyerId, sellerId, mail.getAdId());
        ConversationIndexKey indexKeySellerToBuyer = new ConversationIndexKey(sellerId, buyerId, mail.getAdId());

        ConversationIndexKey indexKeyBuyerToSellerEmail = new ConversationIndexKey(buyerAddress, sellerAddress, mail.getAdId());
        ConversationIndexKey indexKeySellerToBuyerEmail = new ConversationIndexKey(sellerAddress, buyerAddress, mail.getAdId());

        if (!userIdentifierService.getBuyerUserId(headers).isPresent()) {
            LOG.debug("ID Headers are absent for Buyer. Header name {}", userIdentifierService.getBuyerUserIdName());
        }
        if (!userIdentifierService.getSellerUserId(headers).isPresent()) {
            LOG.debug("ID Headers are absent for Seller. Header name {}", userIdentifierService.getSellerUserIdName());
        }

        return tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSeller)
                || tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyer)

                //when tenant switched from email based conversation id to userid based conversation id, we need to check for existing conversation which were email based before the switch
                //TODO:[COMAAS-1163] Remove below 2 checks once all the emails are sent with id headers. We can check for above log messages
                || tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSellerEmail)
                || tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyerEmail)
                ;
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