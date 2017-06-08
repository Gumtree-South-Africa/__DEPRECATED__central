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

import com.ecg.replyts.core.runtime.persistence.mail.*;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration
@Import({
        CassandraPersistenceConfiguration.CassandraClientConfiguration.class,
        RiakPersistenceConfiguration.RiakClientConfiguration.class
})
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
    public MailRepository mailRepository() {
        try {
            return new DiffingRiakMailRepository(bucketNamePrefix, riakClient);
        } catch (RiakRetryFailedException re) {
            throw new RuntimeException("Failed to initialize mail repository", re);
        }
    }

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
