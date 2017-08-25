package com.ecg.de.mobile.replyts.addmetadata;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: beckart
 */
class MailAddMetadataHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MailAddMetadataHandler.class);

    static final String MESSAGE_ID_PLACEHOLDER = "$MESSAGE_ID$";

    static final String CONVERSATION_ID_PLACEHOLDER = "$CONVERSATION_ID$";

    static final String ANONYMIZED_SENDER_ADDRESS = "$ANONYMIZED_SENDER_ADDRESS$";

    private final MessageProcessingContext messageProcessingContext;

    MailAddMetadataHandler(MessageProcessingContext messageProcessingContext) {
        this.messageProcessingContext = messageProcessingContext;

    }

    /**
     * Replaces placeholders with metadata
     * @param content
     * @return
     */
    private String addMetadata(String content) {
        if(content != null) {
            content = content.replace(CONVERSATION_ID_PLACEHOLDER, messageProcessingContext.getConversation().getId());
            content = content.replace(MESSAGE_ID_PLACEHOLDER, messageProcessingContext.getMessageId());
            content = content.replace(ANONYMIZED_SENDER_ADDRESS, messageProcessingContext.getOutgoingMail().getFrom());

        }

        return content;
    }

    /**
     * handles the message to add metadata
     */
    public void handle() {
        try {


            for (TypedContent<String> contentPart : messageProcessingContext.getOutgoingMail().getTextParts(false)) {
                String content = contentPart.getContent();
                contentPart.overrideContent(addMetadata(content));

            }


        } catch (Exception e) {
            LOG.error("Error while adding metadata!", e);
        }
    }
}
