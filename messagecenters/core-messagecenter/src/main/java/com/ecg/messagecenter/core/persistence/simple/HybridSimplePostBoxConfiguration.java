package com.ecg.messagecenter.core.persistence.simple;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
public class HybridSimplePostBoxConfiguration {

    @Value("${migration.postboxes.deepMigration.enabled:false}")
    private Boolean deepMigrationEnabled;

    @Value("${migration.postboxes.delete.cthread.enabled:false}")
    private Boolean postboxDeleteCthreadEnabled;


    @ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
    @Bean
    public RiakSimplePostBoxRepository riakSimplePostBoxRepository() {
        return new DefaultRiakSimplePostBoxRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid-riak-readonly")
    public RiakSimplePostBoxRepository riakReadOnlySimplePostBoxRepository() {
        return new RiakReadOnlySimplePostBoxRepository();
    }

    @Bean
    public CassandraSimplePostBoxRepository cassandraSimplePostBoxRepository(@Qualifier("cassandraSessionForMb") Session cassandraSession, ConsistencyLevel cassandraReadConsistency, ConsistencyLevel cassandraWriteConsistency) {
        return new CassandraSimplePostBoxRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @Primary
    public HybridSimplePostBoxRepository simplePostBoxRepository(RiakSimplePostBoxRepository riakSimplePostBoxRepository,
                                                                 CassandraSimplePostBoxRepository cassandraSimplePostBoxRepository,
                                                                 HybridMigrationClusterState migrationState) {
        return new HybridSimplePostBoxRepository(riakSimplePostBoxRepository, cassandraSimplePostBoxRepository,
                migrationState, deepMigrationEnabled, postboxDeleteCthreadEnabled);
    }

}
