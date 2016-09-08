package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
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
