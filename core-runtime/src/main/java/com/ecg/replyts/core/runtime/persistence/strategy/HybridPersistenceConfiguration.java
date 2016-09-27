package com.ecg.replyts.core.runtime.persistence.strategy;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.CassandraIndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Hybrid persistence configuration which facilitates reading from Riak and writing to both Riak and Cassandra.
 *
 * Uses client-related auto-wired beans from CassandraPersistenceConfiguration.CassandraClientConfiguration and
 * RiakPersistenceConfiguration.RiakClientConfiguration.
 *
 * XXX: Once all migrations are complete, this strategy will become obsolete.
 */
@Configuration
@Import({
  CassandraPersistenceConfiguration.CassandraClientConfiguration.class,
  RiakPersistenceConfiguration.RiakClientConfiguration.class
})
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
public class HybridPersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(HybridPersistenceConfiguration.class);

    // Cassandra

    @Value("${persistence.cassandra.dc:#{null}}")
    private String cassandraDataCenter;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;
    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    // Riak

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String bucketNamePrefix = "";

    @Value("#{'${persistence.riak.bucket.name.prefix:}' != ''}")
    private Boolean useBucketNamePrefix;

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private JacksonAwareObjectMapperConfigurer objectMapperConfigurer;

    @Bean
    public HybridConversationRepository conversationRepository(Session cassandraSession) {
        DefaultCassandraConversationRepository cassandraRepository = new DefaultCassandraConversationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
        RiakConversationRepository riakRepository = useBucketNamePrefix ? new RiakConversationRepository(riakClient, bucketNamePrefix) : new RiakConversationRepository(riakClient);

        cassandraRepository.setObjectMapperConfigurer(objectMapperConfigurer);

        return new HybridConversationRepository(cassandraRepository, riakRepository);
    }

    @Bean
    public ConfigurationRepository configurationRepository(Session cassandraSession) throws RiakRetryFailedException {
        return new CassandraConfigurationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository(Session cassandraSessionForJobs) throws RiakRetryFailedException {
        return new CassandraIndexerClockRepository(cassandraDataCenter, cassandraSessionForJobs);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(Session cassandraSession) {
        return new CassandraCronJobClockRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public MailRepository mailRepository() throws RiakRetryFailedException {
        // Mail storage has been deprecated in Cassandra - only persisting to Riak
        return new DiffingRiakMailRepository(bucketNamePrefix, riakClient);
    }

    @Bean
    public ConversationMigrator conversationMigrator() {
        return null;
    }
}
