package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.indexer.CassandraIndexerClockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexerClockRepository;
import com.ecg.replyts.core.runtime.persistence.conditional.CassandraEnabledConditional;
import com.ecg.replyts.core.runtime.persistence.config.CassandraConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraMailRepository;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

@Configuration
public class CassandraPersistenceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraPersistenceConfiguration.class);

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

    private Collection<InetSocketAddress> cassandraContactPoints;
    private Session cassandraSession;
    private Cluster cassandraCluster;

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

    public ConversationRepository createCassandraConversationRepository() {
        return new CassandraConversationRepository(cassandraSession(), cassandraReadConsistency, cassandraWriteConsistency);
    }

    public ConfigurationRepository createCassandraConfigurationRepository() {
        return new CassandraConfigurationRepository(cassandraSession(), cassandraReadConsistency, cassandraWriteConsistency);
    }

    public MailRepository createCassandraMailRepository() {
        return new CassandraMailRepository(cassandraSession(), cassandraReadConsistency, cassandraWriteConsistency);
    }

    public IndexerClockRepository createCassandraIndexerClockRepository() {
        return new CassandraIndexerClockRepository(cassandraDataCenter, cassandraSession());
    }

    @Bean(name = "cassandraSession")
    @Conditional(CassandraEnabledConditional.class)
    public Session cassandraSession() {
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
        builder.withPoolingOptions(poolingOptions);

        cassandraCluster = builder.build();
        cassandraSession = cassandraCluster.connect(cassandraKeyspace);

        return cassandraSession;
    }

    @PreDestroy
    public void closeCassandra() {
        if (this.cassandraSession != null) {
            this.cassandraSession.close();
        }
        if (this.cassandraCluster != null) {
            this.cassandraCluster.close();
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

    // For testing purposes
    public void setCassandraKeyspace(String cassandraKeyspace) {
        this.cassandraKeyspace = cassandraKeyspace;
    }
}
