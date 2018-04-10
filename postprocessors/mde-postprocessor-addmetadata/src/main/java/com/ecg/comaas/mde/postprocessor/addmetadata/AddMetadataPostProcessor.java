package com.ecg.comaas.mde.postprocessor.addmetadata;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddMetadataPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AddMetadataPostProcessor.class);

    private static final String MESSAGE_ID_PLACEHOLDER = "$MESSAGE_ID$";
    private static final String CONVERSATION_ID_PLACEHOLDER = "$CONVERSATION_ID$";
    private static final String ANONYMIZED_SENDER_ADDRESS = "$ANONYMIZED_SENDER_ADDRESS$";

    private final int orderNumber;

    AddMetadataPostProcessor(int orderNumber) {
        if(orderNumber <= 200) {
            throw new RuntimeException("Order has to higher than 200 to performed after mail cloaking! ");
        }
        this.orderNumber = orderNumber;
    }

    @Override
    public int getOrder() {
        return orderNumber;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        try {
            for (TypedContent<String> contentPart : context.getOutgoingMail().getTextParts(false)) {
                String content = contentPart.getContent();
                contentPart.overrideContent(addMetadata(context, content));
            }
        } catch (Exception e) {
            LOG.error("Error while adding metadata!", e);
        }
    }

    private String addMetadata(MessageProcessingContext context, String content) {
        if (content != null) {
            content = content.replace(CONVERSATION_ID_PLACEHOLDER, context.getConversation().getId());
            content = content.replace(MESSAGE_ID_PLACEHOLDER, context.getMessageId());
            content = content.replace(ANONYMIZED_SENDER_ADDRESS, context.getOutgoingMail().getFrom());
        }

        return content;
    }
}
