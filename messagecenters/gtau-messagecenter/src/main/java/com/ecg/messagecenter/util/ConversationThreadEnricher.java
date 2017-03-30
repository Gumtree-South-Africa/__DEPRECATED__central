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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Initialize or complete existing conversation object. The initial intent was to complement conversation object with anonymous
 * email buyer/seller and status.
 */
@Component
public class ConversationThreadEnricher {
    private final static Timer READ_TIMER = TimingReports.newTimer("postbox-conversation-enricher-read-timer");
    private final static Timer WRITE_TIMER = TimingReports.newTimer("postbox-conversation-enricher-write-timer");

    @Autowired
    private MailCloakingService mailCloakingService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Value("${messages.conversations.enrichment.on.read:false}")
    private boolean shouldEnrichOnRead;

    public ConversationThread enrichOnRead(ConversationThread conversationThread, Optional<Conversation> conversation) {
        if (!shouldEnrichOnRead) {
            return conversationThread;
        }

        try (Timer.Context ignored = READ_TIMER.time()) {
            return enrich(conversationThread, conversation);
        }
    }

    public ConversationThread enrichOnWrite(ConversationThread conversationThread, Optional<Conversation> conversation) {
        try (Timer.Context ignored = WRITE_TIMER.time()) {
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
    private ConversationThread enrich(ConversationThread conversationThread, Optional<Conversation> conversation) {
        if (conversationThread == null ||
              (!Strings.isNullOrEmpty(conversationThread.getBuyerAnonymousEmail().orElse(null)) &&
               !Strings.isNullOrEmpty(conversationThread.getSellerAnonymousEmail().orElse(null)) &&
               !Strings.isNullOrEmpty(conversationThread.getStatus().orElse(null)))) {
            return conversationThread;
        }

        Conversation conv = conversation.orElseGet(() -> conversationRepository.getById(conversationThread.getConversationId()));

        if (conv != null) {
            conversationThread.setBuyerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conv).getAddress()));
            conversationThread.setSellerAnonymousEmail(Optional.ofNullable(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conv).getAddress()));
            conversationThread.setStatus(Optional.ofNullable(conv.getState().name()));
        }

        return conversationThread;
    }
}
