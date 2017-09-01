package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.runtime.logging.MDCConstants.*;

/**
 * Set up a new conversation for an initial contact mail.
 */
@Component
class NewConversationCreator {

    private static final Logger LOG = LoggerFactory.getLogger(NewConversationCreator.class);

    private static final Counter CREATE_COUNTER = TimingReports.newCounter("conversationCreate");

    private final Guids guids;

    private final UniqueConversationSecret uniqueConversationSecret;

    @Autowired
    NewConversationCreator(Guids guids, UniqueConversationSecret uniqueConversationSecret) {
        this.guids = guids;
        this.uniqueConversationSecret = uniqueConversationSecret;
    }

    public void setupNewConversation(MessageProcessingContext context) {

        ConversationStartInfo info = new ConversationStartInfo(context);

        String newConversationId = guids.nextGuid();
        MDC.put(CONVERSATION_ID, newConversationId);
        MDC.put(MAIL_BUYER, info.buyer().getAddress());
        MDC.put(MAIL_SELLER, info.seller().getAddress());

        String buyerSecret = uniqueConversationSecret.nextSecret();
        String sellerSecret = uniqueConversationSecret.nextSecret();

        context.setMessageDirection(MessageDirection.BUYER_TO_SELLER);

        LOG.debug("Creating New Conversation with Buyer({}, secret: {}) and Seller({}. secret: {})", info.buyer().getAddress(), buyerSecret, info.seller().getAddress(), sellerSecret);

        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(newConversationId).
                withAdId(info.adId()).
                withBuyer(info.buyer().getAddress(), buyerSecret).
                withSeller(info.seller().getAddress(), sellerSecret).
                withCustomValues(info.customHeaders()).
                build();
        DefaultMutableConversation newConversation = DefaultMutableConversation.create(newConversationBuilderCommand);
        context.setConversation(newConversation);

        CREATE_COUNTER.inc();
    }

}
