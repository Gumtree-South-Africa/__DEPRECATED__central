package com.ecg.replyts.core.runtime.persistence.strategy;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakFactory;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.raw.pbc.PBClientConfig;
import com.basho.riak.client.raw.pbc.PBClusterConfig;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.RiakIndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig;
import com.ecg.replyts.core.runtime.persistence.config.RiakConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DiffingRiakMailRepository;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "riak")
public class RiakPersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(RiakPersistenceConfiguration.class);

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String bucketNamePrefix = "";

    @Value("#{'${persistence.riak.bucket.name.prefix:}' != ''}")
    private Boolean useBucketNamePrefix;

    @Autowired
    private IRiakClient riakClient;

    @Bean
    public ConversationRepository conversationRepository() {
        return useBucketNamePrefix ? new RiakConversationRepository(riakClient, bucketNamePrefix) : new RiakConversationRepository(riakClient);
    }

    @Bean
    public ConfigurationRepository configurationRepository() throws RiakRetryFailedException {
        return useBucketNamePrefix ? new RiakConfigurationRepository(riakClient, bucketNamePrefix) : new RiakConfigurationRepository(riakClient);
    }

    @Bean
    public MailRepository mailRepository() throws RiakRetryFailedException {
        return new DiffingRiakMailRepository(bucketNamePrefix, riakClient);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository() throws RiakRetryFailedException {
        return new RiakIndexerClockRepository(riakClient, bucketNamePrefix);
    }

    @Bean
    public ConversationMigrator conversationMigrator(RiakConversationRepository conversationRepository) throws RiakRetryFailedException {
        return new ConversationMigrator(conversationRepository, riakClient);
    }

    @Configuration
    @Profile(ReplyTS.PRODUCTIVE_PROFILE)
    public static class RiakClientConfiguration {
        @Value("${persistence.riak.idleConnectionTimeoutMs}")
        private int idleConnectionTtlMs;

        @Value("${persistence.riak.connectionTimeoutMs}")
        private int connectionTimeoutMs;

        @Value("${persistence.riak.requestTimeoutMs}")
        private int requestTimeoutMs;

        @Value("${persistence.riak.connectionPoolSizePerRiakHost}")
        private int connectionPoolSizePerRiakHost;

        @Value("${persistence.riak.maxConnectionsToRiakCluster}")
        private int totalMaxConnectionsToRiakCluster;

        @Autowired
        private RiakHostConfig hostConfig;

        private IRiakClient riakClient;

        @Bean
        public IRiakClient createPrimaryRiakClusterClient () throws RiakException {
            LOG.info("Riak Hosts in primary datacenter: {}", hostConfig.getHostList());

            // The server should only connect to the Riak notes in the same (primary) datacenter.
            List<RiakHostConfig.Host> primaryHostList = hostConfig.getHostList();

            // Note: The parameter totalMaximumConnections makes only sense if max connections for the cluster is smaller
            // that the sum of the pool size per node.
            PBClusterConfig config = new PBClusterConfig(totalMaxConnectionsToRiakCluster);
            PBClientConfig clientConfig = createClientConfigBuilderWithDefaults()
                    .withPoolSize(connectionPoolSizePerRiakHost)
                    .build();

            config.addHosts(clientConfig, primaryHostListAsStringArray());

            return (riakClient = RiakFactory.newClient(config));
        }

        private String[] primaryHostListAsStringArray() {
            return Lists.transform(hostConfig.getHostList(), new Function<RiakHostConfig.Host, String>() {
                @Nullable
                @Override
                public String apply(RiakHostConfig.Host input) {
                    return input.getHost();
                }
            }).toArray(new String[0]);
        }

        private PBClientConfig.Builder createClientConfigBuilderWithDefaults() {
            return new PBClientConfig.Builder()
                    .withConnectionTimeoutMillis(connectionTimeoutMs)
                    .withIdleConnectionTTLMillis(idleConnectionTtlMs)
                    .withInitialPoolSize(0) // we really want this to be 0. otherwise the riak client will not be able to initialize if any riak node is unavailable.
                    .withRequestTimeoutMillis(requestTimeoutMs)
                    .withPort(hostConfig.getProtobufPort());
        }

        @PreDestroy
        public void shutdown() {
            riakClient.shutdown();
        }
    }
}
