package com.ecg.de.mobile.replyts.uniqueid;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * User: beckart
 */
class MailUniqueIdHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MailUniqueIdHandler.class);
    public static final String X_MOBILEDE_BUYER_ID = "X-MOBILEDE-BUYER-ID";

    private final MessageProcessingContext messageProcessingContext;

    private final UniqueIdGenerator uniqueIdGenerator;

    private final Set<String> ignoreEmailAddresses;

    MailUniqueIdHandler(MessageProcessingContext messageProcessingContext, UniqueIdGenerator uniqueIdGenerator, Set<String> ignoreEmailAddresses) {
        this.messageProcessingContext = messageProcessingContext;
        this.uniqueIdGenerator = uniqueIdGenerator;
        this.ignoreEmailAddresses = ignoreEmailAddresses;
    }

    private boolean isSellerDealer() {

        Optional<String> header = Optional.fromNullable(messageProcessingContext.getConversation().getCustomValues().get("seller_type"));
        return header.isPresent() && header.get().equals("DEALER");
    }


    private boolean isMailComingFromBuyer() {

        return messageProcessingContext.getMessageDirection().getFromRole() == ConversationRole.Buyer;

    }


    public void handle() {
        try {

            String buyerMailAddress = messageProcessingContext.getConversation().getUserId(ConversationRole.Buyer);

            if(isSellerDealer() && isMailComingFromBuyer() && !isEmailAddressIgnored(buyerMailAddress)) {

                String originalFromAddress = uniqueIdGenerator.generateUniqueBuyerId(buyerMailAddress);
                messageProcessingContext.getOutgoingMail().addHeader(X_MOBILEDE_BUYER_ID, originalFromAddress);

            }

        } catch (Exception e) {
            LOG.error("Error while generating unique buyer id.", e);
        }
    }

    private boolean isEmailAddressIgnored(String buyerMailAddress) {
        return ignoreEmailAddresses.contains(buyerMailAddress.toLowerCase());
    }


}
