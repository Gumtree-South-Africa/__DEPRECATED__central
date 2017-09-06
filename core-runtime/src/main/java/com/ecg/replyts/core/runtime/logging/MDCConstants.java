package com.ecg.replyts.core.runtime.logging;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.MDC;

public class MDCConstants {

    private MDCConstants() {
        throw new AssertionError();
    }

    public static final String APPLICATION = "application";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String MAIL_ORIGINAL_FROM = "mail_original_from";
    public static final String MAIL_ORIGINAL_TO = "mail_original_to";
    public static final String MAIL_FROM = "mail_from";
    public static final String MAIL_TO = "mail_to";
    public static final String MAIL_DIRECTION = "mail_direction";
    public static final String MESSAGE_ID = "message_id";
    public static final String REVISION = "revision";
    public static final String TENANT = "tenant";

    public static void setContextFields(MessageProcessingContext context) {
        MDC.put(CONVERSATION_ID, context.getConversation().getId());
        MDC.put(MAIL_FROM, context.getSender().getAddress());
        MDC.put(MAIL_TO, context.getRecipient().getAddress());
        MDC.put(MAIL_DIRECTION, context.getMessageDirection().name());
    }
}
