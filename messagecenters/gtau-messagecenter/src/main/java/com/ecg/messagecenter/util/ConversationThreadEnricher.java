package com.ecg.messagecenter.util;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Initialize or complete existing conversation object. The initial intent was to complement conversation object with anonymous
 * email buyer/seller and status.
 */
@Component
public class ConversationThreadEnricher {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadEnricher.class);
    private static final Timer READ_TIMER = TimingReports.newTimer("postbox-conversation-enricher-read-timer");
    private static final Timer WRITE_TIMER = TimingReports.newTimer("postbox-conversation-enricher-write-timer");

    private final MailCloakingService mailCloakingService;
    private final boolean shouldEnrichOnRead;

    @Autowired
    public ConversationThreadEnricher(
            MailCloakingService mailCloakingService,
            @Value("${messages.conversations.enrichment.on.read:false}") boolean shouldEnrichOnRead
    ) {
        this.mailCloakingService = mailCloakingService;
        this.shouldEnrichOnRead = shouldEnrichOnRead;
    }

    public ConversationThread enrichOnRead(ConversationThread conversationThread, Conversation conversation) {
        if (!shouldEnrichOnRead) {
            return conversationThread;
        }

        try (Timer.Context ignored = READ_TIMER.time()) {
            LOG.debug("Enriching conversation thread with conversation id {} and adId {} on read action",
                    conversationThread.getConversationId(), conversationThread.getAdId());
            return enrich(conversationThread, conversation);
        }
    }

    public ConversationThread enrichOnWrite(ConversationThread conversationThread, Conversation conversation) {
        try (Timer.Context ignored = WRITE_TIMER.time()) {
            LOG.debug("Enriching conversation thread with conversation id {} and adId {} on write action",
                    conversationThread.getConversationId(), conversationThread.getAdId());
            return enrich(conversationThread, conversation);
        }
    }

    /**
     * Enriches a {@link ConversationThread} if {@code buyerAnonymousEmail}, {@code sellerAnonymousEmail} or {@code status}
     * are not initialized.
     *
     * @param conversationThread {@link ConversationThread} instance to enrich
     * @return {@link ConversationThread} instance enriched
     */
    private ConversationThread enrich(ConversationThread conversationThread, Conversation conversation) {
        if (conversation == null) {
            LOG.warn("No conversation with id {} and adId {} for conversation thread enrichment provided",
                    conversationThread.getConversationId(), conversationThread.getAdId());
            return conversationThread;
        }

        if (conversationThread == null || !noBuyerOrSellerOrStatus(conversationThread)) {
            return conversationThread;
        }

        conversationThread.setBuyerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress()));
        conversationThread.setSellerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation).getAddress()));
        conversationThread.setStatus(Optional.ofNullable(conversation.getState().name()));

        return conversationThread;
    }

    private static boolean noBuyerOrSellerOrStatus(ConversationThread conversationThread) {
        return Strings.isNullOrEmpty(conversationThread.getBuyerAnonymousEmail().orElse(null)) ||
                Strings.isNullOrEmpty(conversationThread.getSellerAnonymousEmail().orElse(null)) ||
                Strings.isNullOrEmpty(conversationThread.getStatus().orElse(null));
    }
}
