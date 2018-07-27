package com.ecg.comaas.kjca.listener.adcounters;

import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;
import static com.ecg.replyts.core.api.model.mail.Mail.ADID_HEADER;

@ComaasPlugin
@Profile({TENANT_KJCA, TENANT_MVCA})
@Component
class IncrementReplyCountListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(IncrementReplyCountListener.class);

    private TnsApiClient tnsApiClient;

    @Autowired
    IncrementReplyCountListener(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (message.getState() == MessageState.SENT && isInitialPlatformReply(message)) {
            try {
                tnsApiClient.incrementReplyCount(conversation.getAdId());
                LOG.trace("Request for incrementing Ad({}) reply count has completed.", conversation.getAdId());
            } catch (Exception e) {
                LOG.error("Increment reply count failed for Ad {} ", conversation.getAdId(), e);
            }
        }
    }

    private boolean isInitialPlatformReply(Message message) {
        // could be an auto-reattached follow-up if sent from same email
        // for same ad on VIP
        return message.getHeaders().containsKey(ADID_HEADER);
    }
}
