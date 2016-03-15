package com.ecg.replyts.integration.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates a single Cassandra instance
 */
public class EmbeddedCassandra {

    public final static QueryOptions CLUSTER_OPTIONS = new QueryOptions().setRefreshSchemaIntervalMillis(5);
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedCassandra.class);
    private static Set<String> KEYSPACES = new ConcurrentHashSet<>();

    private static EmbeddedCassandra INSTANCE;
    private Cluster cluster;

    private EmbeddedCassandra() {
        // No instantiation
    }

    public static synchronized EmbeddedCassandra getInstance() {
        if (INSTANCE != null) return INSTANCE;
        INSTANCE = new EmbeddedCassandra();
        LOGGER.info("EmbeddedCassandra#getInstance");
        try {
            INSTANCE.start();
        } catch (Exception e) {
            String message = "Failed to start embedded cassandra";
            LOGGER.error(message, e);
            throw new RuntimeException(message);
        }
        return INSTANCE;
    }

    private void start(String config, String tmpDir, long timeout) throws Exception {
        Stopwatch sw = Stopwatch.createStarted();
        EmbeddedCassandraServerHelper.startEmbeddedCassandra(config, tmpDir, timeout);
        LOGGER.info("EmbeddedCassandra#start took {}ms", sw.elapsed(TimeUnit.MILLISECONDS));
    }

    private void start() throws Exception {
        start(EmbeddedCassandraServerHelper.DEFAULT_CASSANDRA_YML_FILE,
                EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR,
                EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT);
    }

    public synchronized Session initStdSchema(String keyspace) {
        return loadSchema(keyspace, "cassandra_schema.cql");
    }

    public synchronized Session loadSchema(String keyspace, String... cqlFilePaths) {
        Stopwatch sw = Stopwatch.createStarted();
        boolean added = KEYSPACES.add(keyspace);
        if (!added) {
            LOGGER.debug("Keyspace '{}' has already been initialized", keyspace);
            return getSession(keyspace);
        }
        // Create a new session
        Session session = getSession(null);
        loadSchema(session, keyspace, cqlFilePaths);

        LOGGER.debug("CassandraRunner#loadSchema took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
        return session;
    }

    private void loadSchema(Session session, String keyspace, String... cqlFilePaths) {
        Preconditions.checkNotNull(cqlFilePaths);
        Preconditions.checkArgument(cqlFilePaths.length >= 1);
        CQLDataLoader cqlDataLoader = new CQLDataLoader(session);
        for (int i = 0; i < cqlFilePaths.length; i++) {
            LOGGER.debug("Loading schema: {}", cqlFilePaths[i]);
            cqlDataLoader.load(new ClassPathCQLDataSet(cqlFilePaths[i], i == 0, false, keyspace));
        }
    }

    public synchronized Session getSession(String host, int nativeTransportPort, String keyspace) {
        if (cluster == null) {
            cluster = new Cluster.Builder().addContactPoints(host)
                    .withPort(nativeTransportPort)
                    .withMaxSchemaAgreementWaitSeconds(1)
                    .withQueryOptions(CLUSTER_OPTIONS)
                    .withoutJMXReporting()
                    .withoutMetrics()
                    .build();
        }
        if (keyspace != null) KEYSPACES.add(keyspace);
        return cluster.connect(keyspace);
    }

    private synchronized Session getSession(String keyspace) {
        Preconditions.checkNotNull(INSTANCE);
        return getSession(EmbeddedCassandraServerHelper.getHost(),
                EmbeddedCassandraServerHelper.getNativeTransportPort(),
                keyspace);
    }

    /**
     * Truncates the tables
     *
     * @param session  a Cassandra Session
     * @param keyspace a database
     */
    public synchronized void cleanTables(Session session, String keyspace) {
        Stopwatch sw = Stopwatch.createStarted();
        ResultSet tables = session.execute("select columnfamily_name from system.schema_columnfamilies where keyspace_name = '" + keyspace + "';");
        tables.forEach(table -> {
            LOGGER.debug("Truncating " + table.getString(0) + " in keyspace[" + keyspace + "]");
            session.execute("TRUNCATE " + table.getString(0));
        });
        LOGGER.info("CassandraRunner#cleanTables took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * This method drops the keyspaces
     */
    public synchronized void cleanServer() {
        Stopwatch sw = Stopwatch.createStarted();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
        KEYSPACES.clear();
        LOGGER.info("CassandraRunner#cleanServer took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
    }

}
