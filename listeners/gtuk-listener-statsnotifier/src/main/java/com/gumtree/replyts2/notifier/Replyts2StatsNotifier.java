package com.gumtree.replyts2.notifier;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.gumtree.replyts2.common.message.GumtreeCustomHeaders.CLIENT_ID;

/**
 * Created by reweber on 16/07/15.
 */
public class Replyts2StatsNotifier implements MessageProcessedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Replyts2StatsNotifier.class);

    private NotificationService notificationService;
    private Boolean sendGoogleAnalyticsEventEnabled;

    public Replyts2StatsNotifier(NotificationService notificationService, Boolean sendGoogleAnalyticsEventEnabled) {
        this.notificationService = notificationService;
        this.sendGoogleAnalyticsEventEnabled = sendGoogleAnalyticsEventEnabled;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        switch (message.getState()) {
            case SENT: if (isFirstMessage(conversation)) {
                    LOGGER.debug("Sending notification to the system");
                    notificationService.notifyReplySuccesfullySent(conversation.getBuyerId(), conversation.getAdId(),
                                                                getClientId(message), sendGoogleAnalyticsEventEnabled);
                    }
                    break;
            default: break;//Do nothing
        }
    }

    private Optional<String> getClientId(Message message) {
        return Optional.ofNullable(message.getHeaders().get(CLIENT_ID.getHeaderValue()));
    }

    private boolean isFirstMessage(Conversation conversation) {
        return conversation.getMessages().size() == 1;
    }

}
