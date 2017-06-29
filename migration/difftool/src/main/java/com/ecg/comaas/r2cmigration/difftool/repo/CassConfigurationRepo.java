package com.ecg.comaas.r2cmigration.difftool.repo;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;

@Repository
public class CassConfigurationRepo {

    private static final Logger LOG = LoggerFactory.getLogger(CassConfigurationRepo.class);

    private Session session;
    private final ConsistencyLevel cassandraReadConsistency;
    private final ConsistencyLevel cassandraWriteConsistency;

    @Autowired
    public CassConfigurationRepo(@Qualifier("cassandraSession") Session session,
                                 @Value("${persistence.cassandra.consistency.read:#{null}}") ConsistencyLevel cassandraReadConsistency,
                                 @Value("${persistence.cassandra.consistency.write:#{null}}") ConsistencyLevel cassandraWriteConsistency) {
        try {
            this.session = session;
            this.cassandraReadConsistency = cassandraReadConsistency;
            this.cassandraWriteConsistency = cassandraWriteConsistency;
        } catch (Exception e) {
            LOG.error("Fail to connect to cassandra: ", e);
            throw new RuntimeException(e);
        }
    }

    public void insertIntoConfiguration(byte[] config) {
        ByteBuffer fileByteBuffer = ByteBuffer.wrap(config);

        Statement insertFile = QueryBuilder.insertInto("core_configuration")
                .value("configuration_key", "config")
                .value("configuration", fileByteBuffer)
                .setConsistencyLevel(cassandraReadConsistency)
                .setSerialConsistencyLevel(cassandraWriteConsistency);
        
        session.execute(insertFile);
    }


}
