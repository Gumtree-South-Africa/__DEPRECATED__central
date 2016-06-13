package com.ecg.replyts2.mailpublisher.kafka;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

class KafkaMailPublisherConditional implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty("mailpublisher.kafka.enabled", Boolean.class, false);
    }
}
