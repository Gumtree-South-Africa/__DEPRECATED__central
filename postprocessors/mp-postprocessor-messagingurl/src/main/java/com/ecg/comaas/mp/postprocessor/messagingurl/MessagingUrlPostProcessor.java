package com.ecg.comaas.mp.postprocessor.messagingurl;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO akobiakov this should probably also be not email specific
@ComaasPlugin
@Component
public class MessagingUrlPostProcessor implements EmailPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingUrlPostProcessor.class);

    private final String MESSAGING_RESOURCE = "/mijnberichten/";
    private final String CONVERSATION_ID_PLACEHOLDER = "conversationIdPlaceholder";

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();

        MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

        LOG.trace("MessagingUrlPostProcessor for message #{}", message.getId());

        List<TypedContent<String>> typedContents = outboundMail.getTextParts(false);
        if (typedContents.isEmpty()) {
            LOG.warn("Message {} has no recognized text parts", message.getId());
        }
        for (TypedContent<String> typedContent : typedContents) {
            if (typedContent.isMutable()) {

                String existingContent = typedContent.getContent();
                String newContent = existingContent.replace(MESSAGING_RESOURCE + CONVERSATION_ID_PLACEHOLDER,
                        MESSAGING_RESOURCE + messageProcessingContext.getConversation().getId());

                typedContent.overrideContent(newContent);
            }
        }

    }

    @Override
    public int getOrder() {
        return 300;
    }
}