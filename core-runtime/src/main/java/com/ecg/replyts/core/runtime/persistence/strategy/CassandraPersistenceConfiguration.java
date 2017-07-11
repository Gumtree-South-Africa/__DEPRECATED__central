package com.ecg.replyts.core.runtime.persistence.strategy;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.runtime.indexer.CassandraIndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.DefaultBlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
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

    @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
    private String cassandraDataCenterForCore;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;
    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    @Bean
    public ConversationRepository conversationRepository(Session cassandraSessionForCore) {
        return new DefaultCassandraConversationRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConfigurationRepository configurationRepository(Session cassandraSessionForCore) {
        return new CassandraConfigurationRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public IndexerClockRepository indexerClockRepository(Session cassandraSessionForJobs) {
        return new CassandraIndexerClockRepository(cassandraDataCenterForCore, cassandraSessionForJobs);
    }

    @Bean
    public CronJobClockRepository cronJobClockRepository(Session cassandraSessionForCore) {
        return new CassandraCronJobClockRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public HeldMailRepository heldMailRepository(Session cassandraSessionForCore) {
        return new CassandraHeldMailRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public BlockUserRepository blockUserRepository(Session cassandraSessionForCore) {
        return new DefaultBlockUserRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    @ConditionalOnExpression("${email.opt.out.enabled:false}")
    public EmailOptOutRepository emailOptOutRepository(Session cassandraSessionForCore) {
        return new EmailOptOutRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
    }

    @Bean
    public ConversationMigrator conversationMigrator() {
        return null;
    }

    @Configuration
    @ConditionalOnExpression("#{'${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid')}")
    public static class CassandraClientConfiguration {

        @Value("${persistence.cassandra.consistency.read:#{null}}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:#{null}}")
        private ConsistencyLevel cassandraWriteConsistency;

        @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
        private String cassandraDataCenterForCore;

        @Value("${persistence.cassandra.core.username:#{null}}")
        private String cassandraUsernameForCore;

        @Value("${persistence.cassandra.core.password:#{null}}")
        private String cassandraPasswordForCore;

        @Value("${persistence.cassandra.core.keyspace:replyts2}")
        private String cassandraKeyspaceForCore;

        @Value("${persistence.cassandra.core.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForCore;

        @Value("${persistence.cassandra.jobs.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForJobs;

        @Value("${persistence.cassandra.core.read.timeout.ms:61000}")
        private Integer readTimeoutMillisForCore;

        private Collection<InetSocketAddress> cassandraContactPointsForCore;

        @Value("${persistence.cassandra.mb.dc:#{systemEnvironment['region']}}")
        private String cassandraDataCenterForMb;

        @Value("${persistence.cassandra.mb.username:#{null}}")
        private String cassandraUsernameForMb;

        @Value("${persistence.cassandra.mb.password:#{null}}")
        private String cassandraPasswordForMb;

        @Value("${persistence.cassandra.mb.keyspace:replyts2}")
        private String cassandraKeyspaceForMb;

        @Value("${persistence.cassandra.mb.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForMb;

        @Value("${persistence.cassandra.mb.read.timeout.ms:61000}")
        private Integer readTimeoutMillisForMb;

        @Value("${persistence.cassandra.slowquerylog.threshold.ms:30000}")
        private long slowQueryThresholdMs;

        private Collection<InetSocketAddress> cassandraContactPointsForMb;

        private Session cassandraSessionForCore;
        private Cluster cassandraClusterForCore;
        private Session cassandraSessionForJobs;
        private Cluster cassandraClusterForJobs;
        private Session cassandraSessionForMb;
        private Cluster cassandraClusterForMb;

        @Value("${persistence.cassandra.core.endpoint:}")
        public void setCassandraEndpointForCore(String cassandraEndpointForCore) {
            cassandraContactPointsForCore = convertToInetSocketAddressCollection(cassandraEndpointForCore);
        }

        @Value("${persistence.cassandra.mb.endpoint:}")
        public void setCassandraEndpointForMb(String cassandraEndpointForMb) {
            cassandraContactPointsForMb = convertToInetSocketAddressCollection(cassandraEndpointForMb);
        }

        private Collection<InetSocketAddress> convertToInetSocketAddressCollection(String cassandraEndpoint) {
            if (StringUtils.hasLength(cassandraEndpoint)) {
                return Splitter.on(',').withKeyValueSeparator(':').split(cassandraEndpoint)
                        .entrySet()
                        .stream()
                        .map(entry -> new InetSocketAddress(entry.getKey().trim(), Integer.valueOf(entry.getValue().trim())))
                        .collect(toList());
            } else {
                return null;
            }
        }

        @Bean(name = "cassandraSessionForCore")
        public Session cassandraSessionForCore() {
            LOG.info("Creating Cassandra session for core");
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForCore, readTimeoutMillisForCore, cassandraDataCenterForCore,
                    cassandraContactPointsForCore, cassandraUsernameForCore, cassandraPasswordForCore, cassandraKeyspaceForCore);

            cassandraClusterForCore = (Cluster) clusterAndSession[0];
            cassandraSessionForCore = (Session) clusterAndSession[1];

            return cassandraSessionForCore;
        }

        @Bean(name = "cassandraSessionForMb")
        public Session cassandraSessionForMb() {
            LOG.info("Creating Cassandra session for message box");
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForMb, readTimeoutMillisForMb, cassandraDataCenterForMb,
                    cassandraContactPointsForMb, cassandraUsernameForMb, cassandraPasswordForMb, cassandraKeyspaceForMb);

            cassandraClusterForMb = (Cluster) clusterAndSession[0];
            cassandraSessionForMb = (Session) clusterAndSession[1];

            return cassandraSessionForMb;
        }

        @Bean(name = "cassandraSessionForJobs")
        public Session cassandraSessionForJobs() {
            LOG.info("Creating Cassandra session for jobs");
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForJobs, readTimeoutMillisForCore, cassandraDataCenterForCore,
                    cassandraContactPointsForCore, cassandraUsernameForCore, cassandraPasswordForCore, cassandraKeyspaceForCore);

            cassandraClusterForJobs = (Cluster) clusterAndSession[0];
            cassandraSessionForJobs = (Session) clusterAndSession[1];

            return cassandraSessionForJobs;
        }

        private Object[] buildClusterAndSession(Integer idleTimeoutSeconds, Integer readTimeoutMillis, String cassandraDataCenter, Collection<InetSocketAddress> cassandraContactPoints,
                                                String cassandraUsername, String cassandraPassword, String cassandraKeyspace) {
            LOG.info("Connecting to Cassandra dc {}, contactpoints {}, user '{}'", cassandraDataCenter, cassandraContactPoints, cassandraUsername);
            LOG.debug("Setting Cassandra readTimeoutMillis to {}", readTimeoutMillis);
            Cluster.Builder builder = Cluster.
                    builder().
                    withSocketOptions(
                            new SocketOptions().
                                    // this sets timeouts for both reads and writes and should be larger than the value
                                    // configured on the Cassandra server
                                            setReadTimeoutMillis(readTimeoutMillis)
                    ).
                    withLoadBalancingPolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(cassandraDataCenter).build()).
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
            if (idleTimeoutSeconds != null) {
                poolingOptions.setIdleTimeoutSeconds(idleTimeoutSeconds);
            }
            builder.withPoolingOptions(poolingOptions);

            Cluster cassandraCluster = builder.build();

            QueryLogger queryLogger = QueryLogger.builder(cassandraCluster).
                    withConstantThreshold(slowQueryThresholdMs).
                    build();
            cassandraCluster.register(queryLogger);

            Session cassandraSession = cassandraCluster.connect(cassandraKeyspace);
            return new Object[]{cassandraCluster, cassandraSession};
        }

        @PreDestroy
        public void closeCassandra() {
            try {
                Closeables.close(cassandraSessionForCore, true);
                Closeables.close(cassandraClusterForCore, true);
                Closeables.close(cassandraSessionForJobs, true);
                Closeables.close(cassandraClusterForJobs, true);
                Closeables.close(cassandraSessionForMb, true);
                Closeables.close(cassandraClusterForMb, true);
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
