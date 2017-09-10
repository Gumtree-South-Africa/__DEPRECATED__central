package com.ecg.replyts.core.runtime.persistence.strategy;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.RiakIndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import({CassandraPersistenceConfiguration.CassandraClientConfiguration.class, RiakPersistenceConfiguration.RiakClientConfiguration.class})
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid")
public class HybridPersistenceConfiguration {
    // Cassandra

    @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
    private String cassandraDataCenter;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;

    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    // Riak

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String bucketNamePrefix = "";

    // Temporary property allowing just the config bucket name to be prefixed - used by GTUK to run against one Riak but two 'config' buckets
    @Value("${persistence.riak.config.bucket.name.prefix:}")
    private String configBucketNamePrefix = "";

    @Value("${persistence.riak.bucket.allowsiblings:true}")
    private boolean allowSiblings;

    @Value("${persistence.riak.bucket.lastwritewins:false}")
    private boolean lastWriteWins;

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private JacksonAwareObjectMapperConfigurer objectMapperConfigurer;

    @Autowired
    private ConversationResumer resumer;

    @Value("${migration.conversations.deepMigration.enabled:false}")
    private boolean deepMigrationEnabled;

    private DefaultCassandraConversationRepository cassandraConversationRepository;

    @Bean
    public MailRepository mailRepository() {
        try {
            return new DiffingRiakMailRepository(bucketNamePrefix, riakClient);
        } catch (RiakRetryFailedException re) {
            throw new RuntimeException("Failed to initialize mail repository", re);
        }
    }

    @Bean
    @Primary
    public HybridConversationRepository conversationRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession, HybridMigrationClusterState migrationState) {
        cassandraConversationRepository = new DefaultCassandraConversationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency, resumer);
        cassandraConversationRepository.setObjectMapperConfigurer(objectMapperConfigurer);

        RiakConversationRepository riakRepository = new RiakConversationRepository(riakClient, bucketNamePrefix, allowSiblings, lastWriteWins);

        return new HybridConversationRepository(cassandraConversationRepository, riakRepository, migrationState, deepMigrationEnabled);
    }

    @Bean
    public CassandraConversationRepository cassandraConversationRepository() {
        return cassandraConversationRepository;
    }

    @Bean
    public ConfigurationRepository configurationRepository() throws RiakRetryFailedException {
        return new RiakConfigurationRepository(riakClient, bucketNamePrefix + configBucketNamePrefix);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository() throws RiakRetryFailedException {
        return new RiakIndexerClockRepository(riakClient, bucketNamePrefix);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(@Qualifier("cassandraSessionForJobs") Session cassandraSession) {
        return new CassandraCronJobClockRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public HeldMailRepository heldMailRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession, MailRepository mailRepository) {
        CassandraHeldMailRepository cassandraHeldMailRepository = new CassandraHeldMailRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
        RiakHeldMailRepository riakHeldMailRepository = new RiakHeldMailRepository(mailRepository);

        return new HybridHeldMailRepository(cassandraHeldMailRepository, riakHeldMailRepository);
    }

    @Bean
    public BlockUserRepository blockUserRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession) {
        return new DefaultBlockUserRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @ConditionalOnExpression("${email.opt.out.enabled:false}")
    public EmailOptOutRepository emailOptOutRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession) {
        return new EmailOptOutRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConversationMigrator conversationMigrator() {
        return null;
    }
}
