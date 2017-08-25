package com.ecg.de.mobile.replyts.rating.listener;

import com.ecg.de.mobile.replyts.rating.svc.DealerRatingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class DealerRatingInviteListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(DealerRatingInviteListener.class);

    private static final String CUSTOM_HEADER_PREFIX = "X-Cust-";

    private final DealerRatingService service;

    public DealerRatingInviteListener(final DealerRatingService service) {
        this.service = service;
    }

    @Override
    public void messageProcessed(final Conversation conversation, final Message message) {
        if (isInitialDealerMessage(message)) {
            LOG.trace("Received initial message, {}", message.getId());
            try {
                service.saveInvitation(message, conversation.getId());
            } catch (Exception e) {
                LOG.error("Error saving invite", e);
            }
        }
    }

    /**
     * Search for any header field that coma service sets.
     * If it is set, the message comes from coma so it's an initial message.
     */
    private boolean isInitialDealerMessage(final Message message) {
        final String key = CUSTOM_HEADER_PREFIX + "Seller_Type";
        return message.getHeaders().containsKey(key)
                && message.getHeaders().get(key).equalsIgnoreCase("DEALER");
    }

}
