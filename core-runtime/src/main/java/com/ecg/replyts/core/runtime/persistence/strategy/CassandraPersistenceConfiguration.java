package com.ecg.replyts.core.runtime.persistence.strategy;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.AtomicMonotonicTimestampGenerator;
import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.LatencyTracker;
import com.datastax.driver.core.PercentileTracker;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.QueryLogger;
import com.datastax.driver.core.ServerSideTimestampGenerator;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import com.datastax.driver.core.policies.PercentileSpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.Policies;
import com.datastax.driver.core.policies.SpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.ecg.replyts.app.preprocessorchain.preprocessors.ConversationResumer;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.runtime.MetricsService;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.DefaultBlockUserRepository;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CassandraCronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultCassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.toList;

@Configuration
public class CassandraPersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraPersistenceConfiguration.class);

    @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
    private String cassandraDataCenterForCore;

    @Autowired
    private ConsistencyLevel cassandraReadConsistency;

    @Autowired
    private ConsistencyLevel cassandraWriteConsistency;

    @Autowired
    private ConversationResumer resumer;

    @Value("${persistence.cassandra.conversations.fetch.size:100}")
    private int conversationEventsFetchSize;

    @Value("${persistence.cassandra.conversations.modified.between.fetch.size:5000}")
    private int conversationsModifiedBetweenFetchSize;

    @Value("${persistence.cassandra.conversations.modified.between.lower.consistency.retry:false}")
    private boolean conversationsModifiedBetweenLowerConsistencyRetry;

    @Bean
    public ConversationRepository conversationRepository(Session cassandraSessionForCore) {
        return new DefaultCassandraConversationRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency,
                resumer, conversationEventsFetchSize, conversationsModifiedBetweenFetchSize, conversationsModifiedBetweenLowerConsistencyRetry);
    }

    @Bean
    public ConfigurationRepository configurationRepository(Session cassandraSessionForCore) {
        return new CassandraConfigurationRepository(cassandraSessionForCore, cassandraReadConsistency, cassandraWriteConsistency);
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

    @Configuration
    public static class CassandraClientConfiguration {

        /**
         * Defines a latency threshold for the percentile tracker used with the speculative execution policy.
         * If the value is too high (e.g. due to a durable outage) there's a risk that the percentile tracker will start
         * providing too high values for p9x, so that the speculative policy becomes effectively disabled.
         * If the value is too low there's a risk for comaas to start firing too many speculative executions overloading
         * the cassandra cluster.
         */
        private static final int HIGHEST_TRACKABLE_LATENCY_MILLIS = 15000;

        // host name to metric name map
        private final ConcurrentMap<String, String> hostMetricsNames = new ConcurrentHashMap<>();

        @Value("${persistence.cassandra.jobs.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForJobs;

        @Value("${persistence.cassandra.consistency.read:LOCAL_QUORUM}")
        private ConsistencyLevel cassandraReadConsistency;

        @Value("${persistence.cassandra.consistency.write:LOCAL_QUORUM}")
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

        @Value("${persistence.cassandra.core.read.timeout.ms:12000}")
        private Integer readTimeoutMillisForCore;

        @Value("${persistence.cassandra.mb.dc:${persistence.cassandra.core.dc:#{systemEnvironment['region']}}}")
        private String cassandraDataCenterForMb;

        @Value("${persistence.cassandra.mb.username:#{null}}")
        private String cassandraUsernameForMb;

        @Value("${persistence.cassandra.mb.password:#{null}}")
        private String cassandraPasswordForMb;

        @Value("${persistence.cassandra.mb.keyspace:replyts2}")
        private String cassandraKeyspaceForMb;

        @Value("${persistence.cassandra.mb.idleTimeoutSeconds:#{null}}")
        private Integer idleTimeoutSecondsForMb;

        @Value("${persistence.cassandra.mb.read.timeout.ms:12000}")
        private Integer readTimeoutMillisForMb;

        @Value("${persistence.cassandra.slowquerylog.threshold.ms:30000}")
        private long slowQueryThresholdMs;

        @Value("${persistence.cassandra.exp.reconnection.delay.base.ms:250}")
        private long expReconnectionPolicyBaseDelay;

        @Value("${persistence.cassandra.exp.reconnection.delay.max.ms:30000}")
        private long expReconnectionPolicyMaxDelay;

        @Value("${persistence.cassandra.retry.never:true}")
        private boolean neverRetry;

        @Value("${persistence.cassandra.speculative.policy.enabled:true}")
        private boolean speculativePolicyEnabled;

        @Value("${persistence.cassandra.speculative.policy.percentile:95.0}")
        private double speculativePolicyPercentile;

        @Value("${persistence.cassandra.speculative.policy.executions:1}")
        private int speculativePolicyMaxExecutions;

        private Collection<InetSocketAddress> cassandraContactPointsForCore;
        private Collection<InetSocketAddress> cassandraContactPointsForMb;

        private Session cassandraSessionForCore;
        private Cluster cassandraClusterForCore;
        private Session cassandraSessionForJobs;
        private Cluster cassandraClusterForJobs;
        private Session cassandraSessionForMb;
        private Cluster cassandraClusterForMb;

        @Value("${persistence.cassandra.core.endpoint}")
        public void setCassandraEndpointForCore(String cassandraEndpointForCore) {
            cassandraContactPointsForCore = convertToInetSocketAddressCollection(cassandraEndpointForCore);
        }

        @Value("${persistence.cassandra.mb.endpoint:${persistence.cassandra.core.endpoint}}")
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

            reportCassandraMetricsWithPrefix(cassandraClusterForCore, "core");

            return cassandraSessionForCore;
        }

        @Bean(name = "cassandraSessionForMb")
        public Session cassandraSessionForMb() {
            LOG.info("Creating Cassandra session for message box");
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForMb, readTimeoutMillisForMb, cassandraDataCenterForMb,
                    cassandraContactPointsForMb, cassandraUsernameForMb, cassandraPasswordForMb, cassandraKeyspaceForMb);

            cassandraClusterForMb = (Cluster) clusterAndSession[0];
            cassandraSessionForMb = (Session) clusterAndSession[1];

            reportCassandraMetricsWithPrefix(cassandraClusterForMb, "mb");

            return cassandraSessionForMb;
        }

        @Bean(name = "cassandraSessionForJobs")
        public Session cassandraSessionForJobs() {
            LOG.info("Creating Cassandra session for jobs");
            Object[] clusterAndSession = buildClusterAndSession(idleTimeoutSecondsForJobs, readTimeoutMillisForCore, cassandraDataCenterForCore,
                    cassandraContactPointsForCore, cassandraUsernameForCore, cassandraPasswordForCore, cassandraKeyspaceForCore);

            cassandraClusterForJobs = (Cluster) clusterAndSession[0];
            cassandraSessionForJobs = (Session) clusterAndSession[1];

            reportCassandraMetricsWithPrefix(cassandraClusterForJobs, "jobs");

            return cassandraSessionForJobs;
        }

        private Object[] buildClusterAndSession(Integer idleTimeoutSeconds, Integer readTimeoutMillis, String cassandraDataCenter, Collection<InetSocketAddress> cassandraContactPoints,
                                                String cassandraUsername, String cassandraPassword, String cassandraKeyspace) {
            LOG.info("Connecting to Cassandra dc {}, contactpoints {}, user '{}'", cassandraDataCenter, cassandraContactPoints, cassandraUsername);
            LOG.debug("Setting Cassandra readTimeoutMillis to {}", readTimeoutMillis);
            Cluster.Builder builder = Cluster.builder()
                    .withSocketOptions(
                            new SocketOptions().
                                    // this sets timeouts for both reads and writes and should be larger than the value
                                    // configured on the Cassandra server
                                            setReadTimeoutMillis(readTimeoutMillis)
                    )
                    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(cassandraDataCenter).build()))
                    // For (250, 30_000) will be: 250, 500, 1000, 2000, 4000, 8000, 16000, 30000, 30000, 30000...
                    .withReconnectionPolicy(new ExponentialReconnectionPolicy(expReconnectionPolicyBaseDelay, expReconnectionPolicyMaxDelay))
                    .withRetryPolicy(neverRetry ? FallthroughRetryPolicy.INSTANCE : DefaultRetryPolicy.INSTANCE)
                    .withSpeculativeExecutionPolicy(buildSpeculativeExecutionPolicy(cassandraContactPoints))
                    // See http://docs.datastax.com/en/developer/java-driver/2.1/manual/speculative_execution/#request-ordering-and-client-timestamps
                    // for explanations why is this necessary when using a speculative policy
                    .withTimestampGenerator(speculativePolicyEnabled ? new AtomicMonotonicTimestampGenerator()
                            : ServerSideTimestampGenerator.INSTANCE)
                    .addContactPointsWithPorts(cassandraContactPoints);

            if (StringUtils.hasLength(cassandraUsername)) {
                builder.withAuthProvider(new PlainTextAuthProvider(cassandraUsername, cassandraPassword));
            }

            // Pooling options in Protocol V3
            PoolingOptions poolingOptions = new PoolingOptions()
                    .setMaxConnectionsPerHost(HostDistance.LOCAL, 10)
                    .setMaxConnectionsPerHost(HostDistance.REMOTE, 2)
                    .setCoreConnectionsPerHost(HostDistance.LOCAL, 2)
                    .setCoreConnectionsPerHost(HostDistance.REMOTE, 1);
            if (idleTimeoutSeconds != null) {
                poolingOptions.setIdleTimeoutSeconds(idleTimeoutSeconds);
            }
            builder.withPoolingOptions(poolingOptions);

            Cluster cassandraCluster = builder.withoutJMXReporting().build();
            QueryLogger queryLogger = QueryLogger.builder(cassandraCluster).
                    withConstantThreshold(slowQueryThresholdMs).
                    build();
            cassandraCluster.register(queryLogger);

            Session cassandraSession = cassandraCluster.connect(cassandraKeyspace);
            return new Object[]{cassandraCluster, cassandraSession};
        }

        private SpeculativeExecutionPolicy buildSpeculativeExecutionPolicy(Collection<InetSocketAddress> cassandraContactPoints) {
            if (!speculativePolicyEnabled) {
                return Policies.defaultSpeculativeExecutionPolicy(); // NOOP speculative policy
            }

            PercentileTracker percentileTracker = IdempotentStatementPercentileTracker
                    .builder(HIGHEST_TRACKABLE_LATENCY_MILLIS) // do not include queries slower than the value into the percentile calculations
                    .build();
            return new PercentileSpeculativeExecutionPolicy(percentileTracker, speculativePolicyPercentile,
                    speculativePolicyMaxExecutions);
        }

        private void reportCassandraMetricsWithPrefix(Cluster cluster, String prefix) {
            String fullPrefix = String.format("%s.cassandra.%s", TimingReports.getHostName(), prefix);
            MetricRegistry comaasRegistry = MetricsService.getInstance().getRegistry();

            LatencyTracker latencyTracker = (host, statement, exception, newLatencyNanos) -> {
                comaasRegistry.histogram(fullPrefix + ".custom.queryLatencyNanos").update(newLatencyNanos);
                comaasRegistry.histogram(fullPrefix + ".custom.queryLatencyNanos." + getHostMetricName(host.getAddress().getHostName())).update(newLatencyNanos);
            };
            cluster.register(latencyTracker);
            cluster.init(); // this is totally safe to do here, check the javadoc
            comaasRegistry.removeMatching((name, metric) -> name != null && name.startsWith(fullPrefix + "."));
            comaasRegistry.register(fullPrefix, cluster.getMetrics().getRegistry());
        }

        String getHostMetricName(String hostName) {
            return hostMetricsNames.computeIfAbsent(hostName, key -> key.split("\\.")[0]);
        }

        @PreDestroy
        public void closeCassandra() {
            List<CloseFuture> closeFutures = new ArrayList<>();

            closeFutures.add(cassandraSessionForCore.closeAsync());
            closeFutures.add(cassandraClusterForCore.closeAsync());
            closeFutures.add(cassandraSessionForJobs.closeAsync());
            closeFutures.add(cassandraClusterForJobs.closeAsync());
            closeFutures.add(cassandraSessionForMb.closeAsync());
            closeFutures.add(cassandraClusterForMb.closeAsync());

            closeFutures.forEach(Futures::getUnchecked);
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
