package com.ecg.messagecenter.util;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This enricher class is aiming to provide helper methods in order to initialize or complete existing conversation object.
 * The first intent of this class was to complement conversation object with anonymous email buyer/seller and status.
 */
@Component
public class ConversationThreadEnricher {

    private final MailCloakingService mailCloakingService;
    private final ConversationRepository conversationRepository;

    private static final Counter CONVERSATION_ENRICHER_COUNTER = TimingReports.newCounter("webapi-conversation-enricher-counts");
    private static final Timer CONVERSATION_ENRICHER_TIMER = TimingReports.newTimer("webapi-conversation-enricher-timer");

    @Autowired
    public ConversationThreadEnricher(MailCloakingService mailCloakingService, ConversationRepository conversationRepository) {
        this.mailCloakingService = mailCloakingService;
        this.conversationRepository = conversationRepository;
    }

    /**
     * Entry point enricher method complementing a {@link ConversationThread} object with additional information.
     *
     * @param conversationThread {@link ConversationThread} instance
     * @param conversation       {@link Conversation} instance
     * @return {@link ConversationThread} instance enriched
     */
    public ConversationThread enrich(ConversationThread conversationThread, Optional<Conversation> conversation) {
        return enrichAnonymousEmailsAndState(conversationThread, conversation);
    }

    /**
     * Enriches a {@link ConversationThread} if {@code buyerAnonymousEmail}, {@code sellerAnonymousEmail} or {@code status}
     * are not initialized.
     *
     * @param conversationThread {@link ConversationThread} instance to enrich
     * @return {@link ConversationThread} instance enriched
     */
    protected ConversationThread enrichAnonymousEmailsAndState(ConversationThread conversationThread, Optional<Conversation> conversation) {

        if (conversationThread == null ||
                (!Strings.isNullOrEmpty(conversationThread.getBuyerAnonymousEmail().orElse(null)) &&
                        !Strings.isNullOrEmpty(conversationThread.getSellerAnonymousEmail().orElse(null)) &&
                        !Strings.isNullOrEmpty(conversationThread.getStatus().orElse(null)))
                ) {
            return conversationThread;
        }

        // count number of time enrichment is being applied
        CONVERSATION_ENRICHER_COUNTER.inc();

        // conversation enricher timer
        Timer.Context timerContext = CONVERSATION_ENRICHER_TIMER.time();

        try {
            Conversation conv = conversation.orElse(conversationRepository.getById(conversationThread.getConversationId()));

            if (conv != null) {
                conversationThread.setBuyerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conv).getAddress()));
                conversationThread.setSellerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conv).getAddress()));
                conversationThread.setStatus(Optional.ofNullable(conv.getState().name()));
            }

        } finally {
            timerContext.stop();
        }

        return conversationThread;
    }
}