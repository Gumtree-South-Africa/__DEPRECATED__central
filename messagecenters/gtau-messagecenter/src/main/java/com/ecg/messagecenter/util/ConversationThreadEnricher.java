package com.ecg.messagecenter.util;

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

    public static final String CONVERSATION_ENRICHER_READ_TIMER_NAME = "postbox-conversation-enricher-read-timer";
    public static final String CONVERSATION_ENRICHER_WRITE_TIMER_NAME = "postbox-conversation-enricher-write-timer";

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
     * Enriches a {@link ConversationThread} object with additional information similarly to {@link #enrich(ConversationThread, Optional)}
     * method in addition to measure the time taken to execute the call.
     *
     * @param conversationThread {@link ConversationThread} instance
     * @param conversation       {@link Conversation} instance
     * @return {@link ConversationThread} instance enriched
     */
    public ConversationThread enrich(ConversationThread conversationThread, Optional<Conversation> conversation, String metricName) {

        Timer.Context timerContext = TimingReports.newTimer(metricName).time();

        try {
            return enrich(conversationThread, conversation);
        } finally {
            timerContext.stop();
        }
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

        Conversation conv = conversation.orElse(conversationRepository.getById(conversationThread.getConversationId()));

        if (conv != null) {
            conversationThread.setBuyerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conv).getAddress()));
            conversationThread.setSellerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conv).getAddress()));
            conversationThread.setStatus(Optional.ofNullable(conv.getState().name()));
        }

        return conversationThread;
    }
}