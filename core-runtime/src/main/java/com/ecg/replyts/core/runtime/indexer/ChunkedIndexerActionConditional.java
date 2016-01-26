package com.ecg.replyts.core.runtime.indexer;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ChunkedIndexerActionConditional implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return "chunked".equals(IndexerActionConditionalHelper.indexerType(context));
    }
}
