package com.ecg.replyts.core.runtime.persistence.strategy;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.CassandraIndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.*;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.HybridConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.QuietReadOnlyRiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
import com.ecg.replyts.core.runtime.persistence.mail.HybridHeldMailRepository;
import com.ecg.replyts.core.runtime.persistence.mail.ReadOnlyRiakMailRepository;
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

@Configuration
@Import({
        CassandraPersistenceConfiguration.CassandraClientConfiguration.class,
        RiakPersistenceConfiguration.RiakClientConfiguration.class
})
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "hybrid-riak-readonly")
public class ReadOnlyRiakHybridPersistenceConfiguration {
    private static final boolean NO_DEEP_MIGRATION = false;

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
    private boolean lastwriteWins;

    @Autowired
    private IRiakClient riakClient;

    @Autowired
    private JacksonAwareObjectMapperConfigurer objectMapperConfigurer;

    @Autowired
    private ConversationResumer resumer;

    @Bean
    public ConversationRepository conversationRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession, HybridMigrationClusterState migrationState) {
        DefaultCassandraConversationRepository cassandraRepository = new DefaultCassandraConversationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency, resumer);
        RiakConversationRepository riakRepository = useBucketNamePrefix ? new QuietReadOnlyRiakConversationRepository(riakClient, bucketNamePrefix, allowSiblings, lastwriteWins) : new RiakConversationRepository(riakClient, allowSiblings, lastwriteWins);

        cassandraRepository.setObjectMapperConfigurer(objectMapperConfigurer);

        return new HybridConversationRepository(cassandraRepository, riakRepository, migrationState, NO_DEEP_MIGRATION);
    }

    @Bean
    public ConfigurationRepository configurationRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession) throws RiakRetryFailedException {
        return new CassandraConfigurationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository(@Qualifier("cassandraSessionForCore") Session cassandraSessionForJobs) throws RiakRetryFailedException {
        return new CassandraIndexerClockRepository(cassandraDataCenter, cassandraSessionForJobs);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(@Qualifier("cassandraSessionForJobs") Session cassandraSession) {
        return new CassandraCronJobClockRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public MailRepository mailRepository() throws RiakRetryFailedException {
        // Mail storage has been deprecated in Cassandra - only persisting to Riak
        return new ReadOnlyRiakMailRepository(bucketNamePrefix, riakClient);
    }

    @Bean
    public HeldMailRepository heldMailRepository(@Qualifier("cassandraSessionForCore") Session cassandraSession, MailRepository mailRepository) {
        CassandraHeldMailRepository cassandraHeldMailRepository = new CassandraHeldMailRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
        RiakHeldMailRepository riakHeldMailRepository = new RiakHeldMailRepository(mailRepository);

        // We can just use the regular HybridHeldMailRepository; it delegates to RiakHeldMailRepository whose write() and
        // remove() implementations are empty on account of delegating read() to mailRepository (see above)

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
