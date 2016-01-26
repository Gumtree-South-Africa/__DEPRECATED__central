package com.ecg.replyts.core.runtime.persistence.conditional;

import org.springframework.context.annotation.ConditionContext;

import java.util.Properties;

public class ConditionalHelper {

    static boolean isCassandraEnabled(ConditionContext context) {
        return isEnabled(context, "persistence.cassandra.enabled", false);
    }

    static boolean isRiakEnabled(ConditionContext context) {
        return isEnabled(context, "persistence.riak.enabled", true);
    }

    private static boolean isEnabled(ConditionContext context, String key, boolean defaultEnabled) {
        Properties replyTsProperties = context.getBeanFactory().getBean("replyts-properties", Properties.class);
        return Boolean.parseBoolean(replyTsProperties.getProperty(key, String.valueOf(defaultEnabled)));
    }
}
