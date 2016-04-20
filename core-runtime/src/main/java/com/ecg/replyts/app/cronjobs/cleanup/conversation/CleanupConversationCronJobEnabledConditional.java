package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class CleanupConversationCronJobEnabledConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Boolean.parseBoolean(context.getEnvironment().getProperty("replyts2.cleanup.conversation.enabled", "false"));
    }
}