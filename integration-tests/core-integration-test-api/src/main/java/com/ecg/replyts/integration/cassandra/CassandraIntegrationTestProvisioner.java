package com.ecg.replyts.integration.cassandra;

import com.datastax.driver.core.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Provisioner to set up Cassandra keyspaces on a Cassandra instance which is expected to be running on localhost.
 */
public class CassandraIntegrationTestProvisioner {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraIntegrationTestProvisioner.class);

    private static final String DEFAULT_KEYSPACE_PREFIX = "test_";

    private static final QueryOptions CLUSTER_OPTIONS = new QueryOptions().setRefreshSchemaIntervalMillis(5);

    private static final String LOCAL_CASSANDRA_HOST = "localhost";
    private static final Integer LOCAL_CASSANDRA_PORT = 9042;

    private static CassandraIntegrationTestProvisioner INSTANCE;

    private Set<String> KEYSPACES = new ConcurrentHashSet<>();

    private Cluster cluster;

    private CassandraIntegrationTestProvisioner() {
    }

    public static synchronized CassandraIntegrationTestProvisioner getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        INSTANCE = new CassandraIntegrationTestProvisioner();

        return INSTANCE;
    }

    public static String createUniqueKeyspaceName() {
        return createUniqueKeyspaceName(DEFAULT_KEYSPACE_PREFIX);
    }

    public static String createUniqueKeyspaceName(String prefix) {
        return (prefix + UUID.randomUUID()).replace('-', '_');
    }

    public synchronized Session initStdSchema(String keyspace) {
        return loadSchema(keyspace, "cassandra_schema.cql");
    }

    public synchronized Session loadSchema(String keyspace, String... cqlFilePaths) {
        Stopwatch sw = Stopwatch.createStarted();

        if (!KEYSPACES.add(keyspace)) {
            LOG.debug("Keyspace '{}' has already been initialized", keyspace);

            return getSession(keyspace);
        }

        // Create a new keyspace-less session and initialize it

        Session session = getSession(null);

        loadSchema(session, keyspace, cqlFilePaths);

        LOG.debug("CassandraRunner#loadSchema took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));

        return session;
    }

    private void loadSchema(Session session, String keyspace, String... cqlFilePaths) {
        Preconditions.checkNotNull(cqlFilePaths);
        Preconditions.checkArgument(cqlFilePaths.length >= 1);

        CassandraIntegrationTestCQLLoader.load(keyspace, true, session, cqlFilePaths);
    }

    private synchronized Session getSession(String keyspace) {
        Preconditions.checkNotNull(INSTANCE);

        return getSession(LOCAL_CASSANDRA_HOST, LOCAL_CASSANDRA_PORT, keyspace);
    }

    private synchronized Session getSession(String host, int nativeTransportPort, String keyspace) {
        if (cluster == null) {
            cluster = new Cluster.Builder().addContactPoints(host)
              .withPort(nativeTransportPort)
              .withMaxSchemaAgreementWaitSeconds(1)
              .withQueryOptions(CLUSTER_OPTIONS)
              .withoutJMXReporting()
              .withoutMetrics()
              .build();
        }

        if (keyspace != null) {
            KEYSPACES.add(keyspace);
        }

        return cluster.connect(keyspace);
    }

    public static Object getEndPoint() {
        return LOCAL_CASSANDRA_HOST + ":" + LOCAL_CASSANDRA_PORT;
    }

    public synchronized void cleanTables(Session session, String keyspace) {
        Stopwatch sw = Stopwatch.createStarted();

        ResultSet tables = session.execute("select columnfamily_name from system.schema_columnfamilies where keyspace_name = '" + keyspace + "';");
        tables.forEach(table -> {
            LOG.debug("Truncating " + table.getString(0) + " in keyspace[" + keyspace + "]");
            session.execute("TRUNCATE " + table.getString(0));
        });

        LOG.info("CassandraRunner#cleanTables took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
    }
}
