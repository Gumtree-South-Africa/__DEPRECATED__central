package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.cronjobs.RiakSimplePostBoxCleanupCronJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid') }")
public class RiakSimplePostBoxConfiguration {
    @Bean
    public RiakSimplePostBoxCleanupCronJob cleanupCronJob() {
        return new RiakSimplePostBoxCleanupCronJob();
    }

    @Bean
    public RiakSimplePostBoxMerger postBoxMerger() {
        return new RiakSimplePostBoxMerger();
    }

    @Bean
    public RiakSimplePostBoxConflictResolver postBoxConflictResolver() {
        return new RiakSimplePostBoxConflictResolver();
    }

    @Bean
    public RiakSimplePostBoxConverter postBoxConverter() {
        return new RiakSimplePostBoxConverter();
    }

    @Configuration
    @ConditionalOnExpression("#{'${persistence.strategy}' == 'riak'}")
    static class RiakOnlyConfiguration {
        @Bean
        public SimplePostBoxRepository postBoxRepository() {
            return new RiakSimplePostBoxRepository();
        }
    }
}