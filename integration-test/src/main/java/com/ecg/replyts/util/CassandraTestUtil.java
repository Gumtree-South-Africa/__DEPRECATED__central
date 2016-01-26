package com.ecg.replyts.util;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraTestUtil.class);

    public static void cleanTables(Session session, String keyspace) {
        ResultSet tables = session.execute("select columnfamily_name from system.schema_columnfamilies where keyspace_name = '" + keyspace + "';");
        tables.forEach ( table -> {
            LOG.info("Truncating "+ table.getString(0) + " in keyspace[" + keyspace + "]");
            session.execute("TRUNCATE " + table.getString(0));
        });
    }

    public static Session newSession(String keyspace) {
        String host = EmbeddedCassandraServerHelper.getHost();
        int port = EmbeddedCassandraServerHelper.getNativeTransportPort();
        Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
        if (keyspace == null) {
            return cluster.newSession();
        } else {
            return cluster.connect(keyspace);
        }
    }

}
