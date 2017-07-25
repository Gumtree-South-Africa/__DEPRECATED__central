package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(value = "replyts.conversationResumeStrategy", havingValue = "id")
public class IdBasedConversationResumer extends ConversationResumer {
    @Autowired
    private UserIdentifierService userIdentifierService;

    public boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context) {
        ConversationStartInfo info = new ConversationStartInfo(context);
        Map<String, String> headers = info.customHeaders();

        String buyerAddress = info.buyer().getAddress();
        String sellerAddress = info.seller().getAddress();

        String buyerId = userIdentifierService.getBuyerUserId(headers).orElse(buyerAddress);
        String sellerId = userIdentifierService.getSellerUserId(headers).orElse(sellerAddress);

        ConversationIndexKey indexKeyBuyerToSeller = new ConversationIndexKey(buyerId, sellerId, info.adId());
        ConversationIndexKey indexKeySellerToBuyer = new ConversationIndexKey(sellerId, buyerId, info.adId());

        return
          tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSeller) ||
          tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyer);
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