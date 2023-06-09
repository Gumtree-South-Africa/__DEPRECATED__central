package com.ecg.messagebox.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraTemplateConfiguration {

    @Bean
    public CassandraTemplate cassandraTemplate(@Qualifier("cassandraSessionForMb") Session session,
                                               @Qualifier("cassandraReadConsistency") ConsistencyLevel readConsistency,
                                               @Qualifier("cassandraWriteConsistency") ConsistencyLevel writeConsistency) {
        return new CassandraTemplate(session, readConsistency, writeConsistency);
    }
}
