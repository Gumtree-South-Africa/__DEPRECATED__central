package com.ecg.replyts.core.runtime.persistence.strategy;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.RiakIndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.DefaultBlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.RiakConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.ecg.replyts.core.runtime.persistence.mail.HybridHeldMailRepository;
import com.ecg.replyts.core.runtime.persistence.mail.RiakHeldMailRepository;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

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

    @Value("${persistence.cassandra.dc:#{systemEnvironment['region']}}")
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

    @Value("${persistence.riak.bucket.allowsiblings:true}")
    private boolean allowSiblings;

    @Value("${persistence.riak.bucket.lastwritewins:false}")
    private boolean lastWriteWins;

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private JacksonAwareObjectMapperConfigurer objectMapperConfigurer;

    @Value("${migration.conversations.deepMigration.enabled:false}")
    private boolean deepMigrationEnabled;

    private DefaultCassandraConversationRepository cassandraConversationRepository;

    @Bean
    @Primary
    public HybridConversationRepository conversationRepository(Session cassandraSession, HybridMigrationClusterState migrationState) {
        cassandraConversationRepository = new DefaultCassandraConversationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
        cassandraConversationRepository.setObjectMapperConfigurer(objectMapperConfigurer);

        RiakConversationRepository riakRepository = useBucketNamePrefix ? new RiakConversationRepository(riakClient, bucketNamePrefix, allowSiblings, lastWriteWins) : new RiakConversationRepository(riakClient, allowSiblings, lastWriteWins);

        return new HybridConversationRepository(cassandraConversationRepository, riakRepository, migrationState, deepMigrationEnabled);
    }

    @Bean
    public CassandraConversationRepository cassandraConversationRepository() {
        return cassandraConversationRepository;
    }

    @Bean
    public ConfigurationRepository configurationRepository() throws RiakRetryFailedException {
        return new RiakConfigurationRepository(riakClient, bucketNamePrefix);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository() throws RiakRetryFailedException {
        return new RiakIndexerClockRepository(riakClient, bucketNamePrefix);
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
    public HeldMailRepository heldMailRepository(Session cassandraSession, MailRepository mailRepository) {
        CassandraHeldMailRepository cassandraHeldMailRepository = new CassandraHeldMailRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
        RiakHeldMailRepository riakHeldMailRepository = new RiakHeldMailRepository(mailRepository);

        return new HybridHeldMailRepository(cassandraHeldMailRepository, riakHeldMailRepository);
    }

    @Bean
    public BlockUserRepository blockUserRepository(Session cassandraSession) {
        return new DefaultBlockUserRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @ConditionalOnExpression("${email.opt.out.enabled:false}")
    public EmailOptOutRepository emailOptOutRepository(Session cassandraSession) {
        return new EmailOptOutRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConversationMigrator conversationMigrator() {
        return null;
    }
}
