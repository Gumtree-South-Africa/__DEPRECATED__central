package com.ecg.de.mobile.replyts.rating.listener;

import com.ecg.de.mobile.replyts.rating.svc.DealerRatingService;
import com.ecg.de.mobile.replyts.rating.svc.SkippingEmailInviteException;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by vbalaramiah on 4/23/15.
 */
public class DealerRatingInviteListener implements MessageProcessedListener {

    public static final String CUSTOM_HEADER_PREFIX = "X-Cust-";

    private static final Logger LOGGER = LoggerFactory.getLogger(DealerRatingInviteListener.class);

    private final DealerRatingService service;

    public DealerRatingInviteListener(DealerRatingService service) {
        this.service = service;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (isInitialDealerMessage(message)) {
            LOGGER.info("Received initial message, " + message.getId());
            try {
                service.saveInvitation(message, conversation.getId());
            } catch (SkippingEmailInviteException e) {
                LOGGER.warn("Skipping invite");
            } catch (Exception e) {
                LOGGER.error("Error saving invite", e);
            }
        }
    }

    protected boolean isInitialDealerMessage(Message message) {
        /**
         * Search for any header field that coma service sets.
         * If it is set, the message comes from coma so it's an initial message.
         */
        final String key = CUSTOM_HEADER_PREFIX + "Seller_Type";
        return message.getHeaders().containsKey(key)
                && message.getHeaders().get(key).equalsIgnoreCase("DEALER");
    }

}
