package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.ConversationGUID;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;

/**
 * Set up a new conversation for an initial contact mail.
 */
@Component
public class NewConversationCreator {

    private static final Logger LOG = LoggerFactory.getLogger(NewConversationCreator.class);

    private static final Counter CREATE_COUNTER = TimingReports.newCounter("conversationCreate");

    private final UniqueConversationSecret uniqueConversationSecret;

    @Autowired
    NewConversationCreator(UniqueConversationSecret uniqueConversationSecret) {
        this.uniqueConversationSecret = uniqueConversationSecret;
    }

    public void setupNewConversation(MessageProcessingContext context) {
        Mail mail = context.getMail().get();

        String buyer = Optional.ofNullable(mail.getReplyTo()).orElse(mail.getFrom());
        String buyerSecret = uniqueConversationSecret.nextSecret();

        String seller = mail.getDeliveredTo();
        String sellerSecret = uniqueConversationSecret.nextSecret();

        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(ConversationGUID.next()).
                withAdId(mail.getAdId()).
                withBuyer(buyer, buyerSecret).
                withSeller(seller, sellerSecret).
                withCustomValues(mail.getCustomHeaders()).
                build();

        DefaultMutableConversation conversation = DefaultMutableConversation.create(newConversationBuilderCommand);
        context.setConversation(conversation);
        context.setMessageDirection(MessageDirection.BUYER_TO_SELLER);

        MDCConstants.setContextFields(context);

        LOG.debug(
            "New Conversation created with Buyer({}, secret: {}) and Seller({}. secret: {})",
            buyer,
            buyerSecret,
            seller,
            sellerSecret);
        CREATE_COUNTER.inc();
    }
}
