package com.ecg.messagecenter.core.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraSimpleMessageCenterRepositoryConfiguration {
    @Bean
    public SimpleMessageCenterRepository postBoxRepository(@Qualifier("cassandraSessionForMb") Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraSimpleMessageCenterRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }
}
