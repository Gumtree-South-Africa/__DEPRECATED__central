package com.ecg.replyts.integration.cassandra;

import com.datastax.driver.core.Session;
import com.ecg.replyts.util.CassandraTestUtil;
import org.apache.commons.io.IOUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbeddedCassandra {

    private final Logger LOGGER = LoggerFactory.getLogger(EmbeddedCassandra.class);
    private final String keyspace;
    private final Pattern TABLE_PATTERN = Pattern.compile("(CREATE TABLE.+?;)");
    private Session session;

    public EmbeddedCassandra(String keyspace) {
        this.keyspace = keyspace;
    }

    public void start(String... cqlFilePaths) throws Exception {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();
        Session session = CassandraTestUtil.newSession(null);
        session.execute("CREATE KEYSPACE " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};");
        session.execute("Use " + keyspace);
        for (String cqlPath : cqlFilePaths) {
            LOGGER.info("Executing statements in {}", cqlPath);
            String startupStatements = IOUtils.toString(getClass().getResourceAsStream(cqlPath)).replaceAll("\\n", " ");
            Matcher matcher = TABLE_PATTERN.matcher(startupStatements);
            while (matcher.find()) {
                String statement = matcher.group(1);
                session.execute(statement);
            }
        }
        this.session = session;

    }

    public Session getSession() {
        return session;
    }

    public void clean() {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

}
