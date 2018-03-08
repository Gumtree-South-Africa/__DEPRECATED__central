package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(value = "conversation.resume.strategy", havingValue = "mail", matchIfMissing = true)
public class MailBasedConversationResumer extends ConversationResumer {
    @Override
    public boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context) {
        Mail mail = context.getMail().get();

        String buyerAddress = Optional.ofNullable(mail.getReplyTo()).orElse(mail.getFrom());
        String sellerAddress = mail.getDeliveredTo();

        ConversationIndexKey indexKeyBuyerToSeller = new ConversationIndexKey(buyerAddress, sellerAddress, mail.getAdId());
        ConversationIndexKey indexKeySellerToBuyer = new ConversationIndexKey(sellerAddress, buyerAddress, mail.getAdId());

        return
          tryResume(repository, context, MessageDirection.BUYER_TO_SELLER, indexKeyBuyerToSeller) ||
          tryResume(repository, context, MessageDirection.SELLER_TO_BUYER, indexKeySellerToBuyer);
    }
}