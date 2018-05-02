package com.ecg.messagecenter.core.persistence.simple;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class RiakSimplePostBoxConfiguration {
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
        public RiakSimplePostBoxRepository postBoxRepository() {
            return new DefaultRiakSimplePostBoxRepository();
        }
    }
}