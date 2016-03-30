package com.ecg.replyts.core.runtime.persistence.conditional;

import org.springframework.context.annotation.ConditionContext;

import java.util.Properties;

public class ConditionalHelper {

    static boolean isCassandraEnabled(ConditionContext context) {
        return isEnabled(context, "persistence.cassandra.enabled", true);
    }

    static boolean isRiakEnabled(ConditionContext context) {
        return isEnabled(context, "persistence.riak.enabled", false);
    }

    private static boolean isEnabled(ConditionContext context, String key, boolean defaultEnabled) {
        return Boolean.parseBoolean(context.getEnvironment().getProperty(key, String.valueOf(defaultEnabled)));
    }
}
