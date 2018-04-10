package com.ecg.comaas.mde.postprocessor.uniqueid;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

class UniqueIdPostProcessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(UniqueIdPostProcessor.class);
    private static final String X_MOBILEDE_BUYER_ID = "X-MOBILEDE-BUYER-ID";

    private UniqueIdGenerator uniqueIdGenerator;
    private final Set<String> ignoredEmailAddresses;
    private final int order;

    UniqueIdPostProcessor(UniqueIdGenerator uniqueIdGenerator, Set<String> ignoreEmailAddressDelimitedList, int order) {
        this.uniqueIdGenerator = uniqueIdGenerator;
        this.ignoredEmailAddresses = ignoreEmailAddressDelimitedList;
        this.order = order;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        try {
            String buyerMailAddress = context.getConversation().getUserId(ConversationRole.Buyer);
            if (isSellerDealer(context) && isMailComingFromBuyer(context) && !isEmailAddressIgnored(buyerMailAddress)) {
                String originalFromAddress = uniqueIdGenerator.generateUniqueBuyerId(buyerMailAddress);
                context.getOutgoingMail().addHeader(X_MOBILEDE_BUYER_ID, originalFromAddress);
            }
        } catch (Exception e) {
            LOG.error("Error while generating unique buyer id.", e);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    private boolean isSellerDealer(MessageProcessingContext context) {
        Optional<String> header = Optional.ofNullable(context.getConversation().getCustomValues().get("seller_type"));
        return header.isPresent() && header.get().equals("DEALER");
    }

    private boolean isMailComingFromBuyer(MessageProcessingContext context) {
        return context.getMessageDirection().getFromRole() == ConversationRole.Buyer;
    }

    private boolean isEmailAddressIgnored(String buyerMailAddress) {
        return ignoredEmailAddresses.contains(buyerMailAddress.toLowerCase());
    }
}
