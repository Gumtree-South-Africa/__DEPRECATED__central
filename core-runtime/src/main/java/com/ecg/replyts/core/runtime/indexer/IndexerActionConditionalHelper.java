package com.ecg.replyts.core.runtime.indexer;

import org.springframework.context.annotation.ConditionContext;

public class IndexerActionConditionalHelper {

    public static String indexerType(ConditionContext context) {
        return context.getEnvironment().getProperty("replyts.indexer.type", "chunked");
    }
}
