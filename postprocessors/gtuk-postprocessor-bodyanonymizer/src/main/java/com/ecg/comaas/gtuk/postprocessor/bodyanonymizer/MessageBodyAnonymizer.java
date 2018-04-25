package com.ecg.comaas.gtuk.postprocessor.bodyanonymizer;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

/**
 * Examines the body of the message and replaces all instances of seller and
 * buyer email addresses with their anonymized equivalents. Buyer's email is
 * untouched when the seller's replying and vice versa.
 *
 * Does not do anything if anonymization is disabled. See {@link Addresser}.
 */
@ComaasPlugin
@Profile(TENANT_GTUK)
@Component
@Import(MessageBodyAnonymizerConfig.class)
public class MessageBodyAnonymizer implements PostProcessor {

    private HideEmailHandler hideEmailHandler;
    private RevealEmailHandler revealEmailHandler;
    private SafetyTextHandler safetyTextHandler;

    @Autowired
    public MessageBodyAnonymizer(MessageBodyAnonymizerConfig messageBodyAnonymizerConfig, MailCloakingService mailCloakingService) {
        this.hideEmailHandler = new HideEmailHandler(messageBodyAnonymizerConfig, mailCloakingService);
        this.revealEmailHandler = new RevealEmailHandler(messageBodyAnonymizerConfig);
        this.safetyTextHandler = new SafetyTextHandler(messageBodyAnonymizerConfig);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        processInOrder(context);
    }

    private void processInOrder(MessageProcessingContext context) {
        Conversation conversation = context.getConversation();
        Message message = context.getMessage();
        Mail mail = context.getOutgoingMail();

        hideEmailHandler.process(context);
        revealEmailHandler.process(conversation, message, mail);
        safetyTextHandler.process(conversation, message, mail);
    }

    @Override
    public int getOrder() {
        return 300;
    }
}