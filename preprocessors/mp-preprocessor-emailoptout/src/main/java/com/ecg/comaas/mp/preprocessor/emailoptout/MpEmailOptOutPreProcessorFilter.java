package com.ecg.comaas.mp.preprocessor.emailoptout;

import com.ecg.replyts.app.preprocessorchain.preprocessors.EmailOptOutPreProcessorFilter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_BE;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;

/**
 * Suma klussen (jobs) ads only support email and not messaging,
 * so must skip email opt out logic for emails belonging to klussen ads.
 */
@ComaasPlugin
@Profile({TENANT_MP, TENANT_BE})
@Component
public class MpEmailOptOutPreProcessorFilter implements EmailOptOutPreProcessorFilter {

    @Override
    public boolean filter(MessageProcessingContext context) {
        Conversation c = context.getConversation();
        return c.getAdId().startsWith("k");

        // TODO akobiakov: MP sync
    }
}