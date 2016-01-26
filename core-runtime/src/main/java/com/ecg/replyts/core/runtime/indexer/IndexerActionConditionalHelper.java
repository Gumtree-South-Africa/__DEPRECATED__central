package com.ecg.replyts.core.runtime.indexer;

import org.springframework.context.annotation.ConditionContext;

import java.util.Properties;

public class IndexerActionConditionalHelper {

    public static String indexerType(ConditionContext context) {
        Properties replyTsProperties = context.getBeanFactory().getBean("replyts-properties", Properties.class);
        return replyTsProperties.getProperty("replyts.indexer.type", "chunked");
    }
}
