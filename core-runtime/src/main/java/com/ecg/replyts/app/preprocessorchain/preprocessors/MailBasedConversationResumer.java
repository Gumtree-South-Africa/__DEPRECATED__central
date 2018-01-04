package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "conversation.resume.strategy", havingValue = "mail", matchIfMissing = true)
public class MailBasedConversationResumer extends ConversationResumer {
    public boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context) {
        ConversationStartInfo info = new ConversationStartInfo(context);

        String buyerAddress = info.buyer().getAddress();
        String sellerAddress = info.seller().getAddress();

        ConversationIndexKey indexKeyBuyerToSeller = new ConversationIndexKey(buyerAddress, sellerAddress, info.adId());
        ConversationIndexKey indexKeySellerToBuyer = new ConversationIndexKey(sellerAddress, buyerAddress, info.adId());

        return
          tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSeller) ||
          tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyer);
    }
}