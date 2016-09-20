package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
public class HybridSimplePostBoxConfiguration {


    @Bean
    @ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
    @Qualifier("RiakSimplePostBoxRepository")
    public SimplePostBoxRepository riakSimplePostBoxRepository() {
        return new RiakSimplePostBoxRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid-riak-readonly")
    @Qualifier("RiakSimplePostBoxRepository")
    public SimplePostBoxRepository riakReadOnlySimplePostBoxRepository() {
        return new RiakReadOnlySimplePostBoxRepository();
    }

    @Bean
    public CassandraSimplePostBoxRepository cassandraSimplePostBoxRepository(Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraSimplePostBoxRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @Primary
    @Qualifier("RiakSimplePostBoxRepository")
    public HybridSimplePostBoxRepository postBoxRepository(@Qualifier("RiakSimplePostBoxRepository") SimplePostBoxRepository riakRepository,
                                                     CassandraSimplePostBoxRepository cassandraRepository) {
        return new HybridSimplePostBoxRepository(riakRepository, cassandraRepository);
    }
}
