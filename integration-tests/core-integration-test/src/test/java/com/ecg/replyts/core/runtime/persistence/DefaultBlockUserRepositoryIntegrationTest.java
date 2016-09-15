package com.ecg.replyts.core.runtime.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class DefaultBlockUserRepositoryIntegrationTest {
    private static String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private static Session session = null;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    private static BlockUserRepository conversationRepository;

    @BeforeClass
    public static void init() {
        if (session == null) {
            session = casdb.initStdSchema(KEYSPACE);
        }

        conversationRepository = new DefaultBlockUserRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Test
    public void blockUser() {
        conversationRepository.blockUser("reporterId", "blockedId");

        Optional<BlockedUserInfo> blockedUserInfo = conversationRepository.getBlockedUserInfo("reporterId", "blockedId");

        assertEquals(true, blockedUserInfo.isPresent());
        assertEquals("reporterId", blockedUserInfo.get().getReporterUserId());
        assertEquals("blockedId", blockedUserInfo.get().getBlockedUserId());
    }

    @Test
    public void unblockUser() {
        conversationRepository.blockUser("reporterId", "blockedId");

        conversationRepository.unblockUser("reporterId", "blockedId");

        Optional<BlockedUserInfo> blockedUserInfo = conversationRepository.getBlockedUserInfo("reporterId", "blockedId");

        assertEquals(false, blockedUserInfo.isPresent());
    }

}
