package com.ecg.replyts.core.runtime.logging;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.cluster.XidFactory;
import org.slf4j.MDC;

import java.util.Optional;

public class MDCConstants {

    private MDCConstants() {
        throw new AssertionError();
    }

    public static final String APPLICATION_NAME = "application";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String CORRELATION_ID = "correlation_id";
    public static final String FILENAME = "filename";
    public static final String MAIL_ORIGINAL_FROM = "mail_original_from";
    public static final String MAIL_ORIGINAL_TO = "mail_original_to";
    public static final String MAIL_FROM = "mail_from";
    public static final String MAIL_TO = "mail_to";
    public static final String MAIL_DIRECTION = "mail_direction";
    public static final String MESSAGE_ID = "message_id";
    public static final String VERSION = "version";
    public static final String TASK_NAME = "task_name";
    public static final String TENANT = "tenant";
    public static final String IS_SSL = "is_ssl";

    public static void setContextFields(MessageProcessingContext context) {
        MDC.put(CONVERSATION_ID, context.getConversation().getId());
        MDC.put(MAIL_FROM, context.getSender().getAddress());
        MDC.put(MAIL_TO, context.getRecipient().getAddress());
        MDC.put(MAIL_DIRECTION, context.getMessageDirection().name());
    }

    public static void setTaskFields(String taskName) {
        MDC.clear();
        MDC.put(CORRELATION_ID, XidFactory.nextXid());
        MDC.put(TASK_NAME, taskName);
    }

    public static Runnable setTaskFields(Runnable runnable, String taskName) {
        Optional<String> correlationId = Optional.ofNullable(MDC.get(CORRELATION_ID));
        return () -> {
            MDC.clear();
            MDC.put(CORRELATION_ID, correlationId.orElseGet(XidFactory::nextXid));
            MDC.put(TASK_NAME, taskName);
            runnable.run();
        };
    }
}
