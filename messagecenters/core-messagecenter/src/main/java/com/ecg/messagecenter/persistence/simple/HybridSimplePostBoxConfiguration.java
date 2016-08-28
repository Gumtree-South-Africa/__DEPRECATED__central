package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
public class HybridSimplePostBoxConfiguration {
    @Bean
    public RiakSimplePostBoxRepository riakSimplePostBoxRepository() {
        return new RiakSimplePostBoxRepository();
    }

    @Bean
    public CassandraSimplePostBoxRepository cassandraSimplePostBoxRepository(Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraSimplePostBoxRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @Primary
    public SimplePostBoxRepository postBoxRepository(RiakSimplePostBoxRepository riakRepository, CassandraSimplePostBoxRepository cassandraRepository) {
        return new HybridSimplePostBoxRepository(riakRepository, cassandraRepository);
    }
}
