package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailOutOutRepositoryIntegrationTest {
    private static String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private static Session session = null;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    private static EmailOptOutRepository emailOutRepo;

    @BeforeClass
    public static void init() {
        if (session == null) {
            session = casdb.initStdSchema(KEYSPACE);
        }

        emailOutRepo = new EmailOptOutRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Test
    public void turnOffOnEmail() {
        assertTrue(emailOutRepo.isEmailTurnedOn("1"));

        emailOutRepo.turnOffEmail("1");

        assertFalse(emailOutRepo.isEmailTurnedOn("1"));

        emailOutRepo.turnOnEmail("1");

        assertTrue(emailOutRepo.isEmailTurnedOn("1"));
    }

}
