package com.ecg.replyts.integration.cassandra;

import com.datastax.driver.core.Session;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class CassandraIntegrationTestCQLLoader {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraIntegrationTestCQLLoader.class);

    public static void load(String keyspace, boolean createKeyspace, Session session, String... paths) {
        if (createKeyspace) {
            createAndUseKeyspace(session, keyspace);
        }

        for(String path : paths) {
            if (!path.startsWith("/"))
                path = "/" + path;

            LOG.debug("executing statements from : " + path);

            for (String statement : extractStatements(path)) {
                LOG.debug("executing : " + statement);

                session.execute(statement);
            }
        }
    }

    private static List<String> parseStatements(List<String> lines) {
        return new CassandraIntegrationTestCQLLexer(lines).getStatements();
    }

    public static List<String> extractStatements(String path) {
        InputStream inputStream = CassandraIntegrationTestCQLLoader.class.getResourceAsStream(path);

        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
            return parseStatements(buffer.lines().filter(line -> StringUtils.isNotBlank(line)).collect(Collectors.toList()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read statements from path " + path, e);
        }
    }

    private static void createAndUseKeyspace(Session session, String keyspace) {
        String createQuery = "CREATE KEYSPACE " + keyspace + " WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}";

        LOG.debug("executing : " + createQuery);

        session.execute(createQuery);

        String useQuery = "USE " + keyspace;

        LOG.debug("executing : " + useQuery);

        session.execute(useQuery);
    }
}
