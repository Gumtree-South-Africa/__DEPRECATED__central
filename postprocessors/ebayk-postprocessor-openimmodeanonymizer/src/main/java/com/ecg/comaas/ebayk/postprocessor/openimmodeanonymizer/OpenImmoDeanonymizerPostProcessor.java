package com.ecg.comaas.ebayk.postprocessor.openimmodeanonymizer;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Component
public class OpenImmoDeanonymizerPostProcessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(OpenImmoDeanonymizerPostProcessor.class);

    private static final int LAST_LISTENER = 200;

    static final String AD_API_USER_ID = "X-Cust-Ad-Api-User-Id";

    private static final MailAddress FROM = new MailAddress("noreply-immobilien@ebay-kleinanzeigen.de");

    @Override
    public void postProcess(MessageProcessingContext context) {
        if (!mailToOpenImmoAd(context)) {
            return;
        }

        try {
            Conversation c = context.getConversation();
            MailAddress replyTo = new MailAddress(c.getUserIdFor(context.getMessageDirection().getFromRole()));
            MutableMail outgoingMail = context.getOutgoingMail();

            outgoingMail.setFrom(FROM);
            outgoingMail.setReplyTo(replyTo);
        } catch (Exception e) {
            LOG.error("Error deanonymizing Mail address for Open-Immo Conversation #" + context.getConversation().getId());
            throw new RuntimeException(e);
        }
    }

    private boolean mailToOpenImmoAd(MessageProcessingContext context) {
        if (context.getMail().get().getUniqueHeader(AD_API_USER_ID) == null) {
            return false;
        }
        return Range.closed(20000, 200000).contains(Integer.parseInt(context.getMail().get().getUniqueHeader(AD_API_USER_ID)));
    }

    @Override
    public int getOrder() {
        return LAST_LISTENER;
    }
}
