package com.ecg.comaas.mp.postprocessor.messagingurl;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.*;

// TODO akobiakov this should probably also be not email specific
@ComaasPlugin
@Profile({TENANT_MP, TENANT_BE, TENANT_KJCA})
@Component
public class MessagingUrlPostProcessor implements EmailPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingUrlPostProcessor.class);

    private final String CONVERSATION_ID_PLACEHOLDER = "conversationIdPlaceholder";
    private final String messagingResource;
    private final String replaceString;

    MessagingUrlPostProcessor(
            @Value("${comaas.postprocessor.messaging_url}") String messagingResource) {
        this.messagingResource = messagingResource;
        this.replaceString = messagingResource + CONVERSATION_ID_PLACEHOLDER;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();

        MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

        LOG.debug("MessagingUrlPostProcessor for message #{}", message.getId());

        List<TypedContent<String>> typedContents = outboundMail.getTextParts(false);
        if (typedContents.isEmpty()) {
            LOG.warn("Message {} has no recognized text parts", message.getId());
        }
        for (TypedContent<String> typedContent : typedContents) {
            if (typedContent.isMutable()) {

                String existingContent = typedContent.getContent();

                if (existingContent.contains(replaceString)) {
                    String newContent = existingContent.replace(messagingResource + CONVERSATION_ID_PLACEHOLDER,
                            messagingResource + messageProcessingContext.getConversation().getId());

                    typedContent.overrideContent(newContent);
                }

            } else {
                LOG.warn("Message {} is immutable cannot change its content ", message.getId());
            }
        }
    }

    @Override
    public int getOrder() {
        return 300;
    }
}