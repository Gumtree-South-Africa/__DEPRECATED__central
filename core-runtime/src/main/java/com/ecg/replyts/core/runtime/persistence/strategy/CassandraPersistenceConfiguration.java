package com.ecg.replyts.core.runtime.persistence.strategy;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.CassandraIndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.DefaultCassandraMailRepository;
import com.ecg.replyts.migrations.cleanupoptimizer.ConversationMigrator;
import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

@Configuration
@ConditionalOnProperty(name = "persistence.strategy", havingValue = "cassandra")
public class CassandraPersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraPersistenceConfiguration.class);

    @Value("${persistence.cassandra.dc:#{null}}")
    private String cassandraDataCenter;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;
    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    @Bean
    public ConversationRepository conversationRepository(Session cassandraSession) {
        return new DefaultCassandraConversationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConfigurationRepository configurationRepository(Session cassandraSession) {
        return new CassandraConfigurationRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public MailRepository mailRepository(Session cassandraSession) {
        return new DefaultCassandraMailRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository(Session cassandraSessionForJobs) {
        return new CassandraIndexerClockRepository(cassandraDataCenter, cassandraSessionForJobs);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(Session cassandraSession) {
        return new CassandraCronJobClockRepository(cassandraSession, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConversationMigrator conversationMigrator() {
        return null;
    }

    @Configuration
    @ConditionalOnExpression("#{'${persistence.strategy}' == 'cassandra' || '${persistence.strategy}' == 'hybrid'}")
    public static class CassandraClientConfiguration {
        @Value("${persistence.cassandra.dc:#{null}}")
        private String cassandraDataCenter;

        @Value("${persistence.cassandra.username:#{null}}")
        private String cassandraUsername;

        @Value("${persistence.cassandra.password:#{null}}")
        private String cassandraPassword;

        @Value("${persistence.cassandra.keyspace:#{null}}")
        private String cassandraKeyspace;

        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Value("${persistence.cassandra.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSeconds;

        @Value("${persistence.cassandra.jobs.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForJobs;

        private Collection<InetSocketAddress> cassandraContactPoints;
        private Session cassandraSession;
        private Cluster cassandraCluster;
        private Session cassandraSessionForJobs;
        private Cluster cassandraClusterForJobs;

        @Value("${persistence.cassandra.endpoint:}")
        public void setCassandraEndpoint(String cassandraEndpoint) {
            if (StringUtils.hasLength(cassandraEndpoint)) {
                cassandraContactPoints = Splitter.on(',').withKeyValueSeparator(':').split(cassandraEndpoint)
                        .entrySet()
                        .stream()
                        .map(entry -> new InetSocketAddress(entry.getKey().trim(), Integer.valueOf(entry.getValue().trim())))
                        .collect(toList());
            }
        }

        @Bean(name = "cassandraSession")
        public Session cassandraSession() {
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSeconds);

            cassandraCluster = (Cluster) clusterAndSession[0];
            cassandraSession = (Session) clusterAndSession[1];

            return cassandraSession;
        }

        @Bean(name = "cassandraSessionForJobs")
        public Session cassandraSessionForJobs() {
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForJobs);

            cassandraClusterForJobs = (Cluster) clusterAndSession[0];
            cassandraSessionForJobs = (Session) clusterAndSession[1];

            return cassandraSessionForJobs;
        }

        private Object[] buildClusterAndSession(Integer idleTimeoutSeconds) {
            LOG.info("Connecting to Cassandra dc {}, contactpoints {}, user '{}'", cassandraDataCenter, cassandraContactPoints, cassandraUsername);
            Cluster.Builder builder = Cluster.
                    builder().
                    withLoadBalancingPolicy(new DCAwareRoundRobinPolicy(cassandraDataCenter)).
                    addContactPointsWithPorts(cassandraContactPoints);
            if (StringUtils.hasLength(cassandraUsername)) {
                builder.withAuthProvider(new PlainTextAuthProvider(cassandraUsername, cassandraPassword));
            }

            // Pooling options in Protocol V3
            PoolingOptions poolingOptions = new PoolingOptions();
            poolingOptions.setMaxConnectionsPerHost(HostDistance.LOCAL, 10);
            poolingOptions.setMaxConnectionsPerHost(HostDistance.REMOTE, 2);
            poolingOptions.setCoreConnectionsPerHost(HostDistance.LOCAL, 2);
            poolingOptions.setCoreConnectionsPerHost(HostDistance.REMOTE, 1);
            if (idleTimeoutSeconds != null) poolingOptions.setIdleTimeoutSeconds(idleTimeoutSeconds);
            builder.withPoolingOptions(poolingOptions);

            Cluster cassandraCluster = builder.build();
            Session cassandraSession = cassandraCluster.connect(cassandraKeyspace);

            return new Object[]{cassandraCluster, cassandraSession};
        }

        @PreDestroy
        public void closeCassandra() {
            try {
                Closeables.close(cassandraSession, true);
                Closeables.close(cassandraCluster, true);
                Closeables.close(cassandraSessionForJobs, true);
                Closeables.close(cassandraClusterForJobs, true);
            } catch (IOException ignored) {
            }
        }

        @Bean(name = "cassandraReadConsistency")
        public ConsistencyLevel getCassandraReadConsistency() {
            return cassandraReadConsistency;
        }

        @Bean(name = "cassandraWriteConsistency")
        public ConsistencyLevel getCassandraWriteConsistency() {
            return cassandraWriteConsistency;
        }
    }
}
