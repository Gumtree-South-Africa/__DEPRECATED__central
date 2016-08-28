package com.ecg.messagecenter.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "cassandra")
public class CassandraSimplePostBoxConfiguration {
    @Bean
    public SimplePostBoxRepository postBoxRepository(Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraSimplePostBoxRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }
}
