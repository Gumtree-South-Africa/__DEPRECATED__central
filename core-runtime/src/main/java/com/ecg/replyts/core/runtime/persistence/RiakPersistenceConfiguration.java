package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.RiakIndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.RiakConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiakPersistenceConfiguration {

    @Autowired(required = false)
    private IRiakClient riakClient;

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String bucketNamePrefix = "";

    public ConversationRepository createRiakConversationRepository() {
        return useBucketNamePrefix() ? new RiakConversationRepository(riakClient, bucketNamePrefix) : new RiakConversationRepository(riakClient);
    }

    public ConfigurationRepository createRiakConfigurationRepository() throws RiakRetryFailedException {
        return useBucketNamePrefix() ? new RiakConfigurationRepository(riakClient, bucketNamePrefix) : new RiakConfigurationRepository(riakClient);
    }

    public MailRepository createRiakMailRepository() throws RiakRetryFailedException {
        return new DiffingRiakMailRepository(bucketNamePrefix, riakClient);
    }

    public IndexerClockRepository createRiakIndexerClockRepository() throws RiakRetryFailedException {
        return new RiakIndexerClockRepository(riakClient, bucketNamePrefix);
    }

    public ConversationMigrator createRiakConversationMigrator(ConversationRepository conversationRepository) throws RiakRetryFailedException {
        return new ConversationMigrator(conversationRepository, riakClient);
    }

    private boolean useBucketNamePrefix() {
        return bucketNamePrefix != null && !bucketNamePrefix.isEmpty();
    }

}
