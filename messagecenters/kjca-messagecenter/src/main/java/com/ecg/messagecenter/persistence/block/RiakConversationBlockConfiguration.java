package com.ecg.messagecenter.persistence.block;

import com.ecg.messagecenter.cronjobs.ConversationBlockCleanupCronJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class RiakConversationBlockConfiguration {
    @Bean
    public ConversationBlockCleanupCronJob cleanupCronJob() {
        return new ConversationBlockCleanupCronJob();
    }

    @Bean
    public RiakConversationBlockConflictResolver conversationBlockConflictResolver() {
        return new RiakConversationBlockConflictResolver();
    }

    @Bean
    public RiakConversationBlockConverter conversationBlockConverter() {
        return new RiakConversationBlockConverter();
    }

    @Bean
    public ConversationBlockRepository conversationBlockRepository() {
        return new RiakConversationBlockRepository();
    }
}