package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.conditional.CassandraEnabledConditional;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CassandraPersistenceConfiguration.class, RiakPersistenceConfiguration.class})
public class PersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfiguration.class);

    @Autowired
    private CassandraPersistenceConfiguration cassandra;
    @Autowired
    private RiakPersistenceConfiguration riak;

    @Value("${persistence.cassandra.enabled:false}")
    private boolean cassandraEnabled;

    @Bean
    public ConversationRepository conversationRepository() throws RiakRetryFailedException {
        if (cassandraEnabled) return cassandra.createCassandraConversationRepository();
        else return riak.createRiakConversationRepository();
    }

    @Bean
    public ConfigurationRepository configurationRepository() throws RiakRetryFailedException {
        if (cassandraEnabled) return cassandra.createCassandraConfigurationRepository();
        return riak.createRiakConfigurationRepository();
    }

    @Bean
    public MailRepository mailRepository() throws RiakRetryFailedException {
        if (cassandraEnabled) return cassandra.createCassandraMailRepository();
        return riak.createRiakMailRepository();
    }

    @Bean
    public IndexerClockRepository indexerClockRepository() throws RiakRetryFailedException {
        if (cassandraEnabled) return cassandra.createCassandraIndexerClockRepository();
        return riak.createRiakIndexerClockRepository();
    }

    @Bean
    @Conditional(value = CassandraEnabledConditional.class)
    public CronJobClockRepository cronJobClockRepository() {
        return cassandra.createCassandraCronJobClockRepository();
    }

    @Bean
    public ConversationMigrator conversationMigrator() throws RiakRetryFailedException {
        if (cassandraEnabled) return null;
        return riak.createRiakConversationMigrator(conversationRepository());
    }

}
