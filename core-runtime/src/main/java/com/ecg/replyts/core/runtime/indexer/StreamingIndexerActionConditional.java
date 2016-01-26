package com.ecg.replyts.core.runtime.indexer;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class StreamingIndexerActionConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return "streaming".equals(IndexerActionConditionalHelper.indexerType(context));
    }
}