package com.ecg.replyts.core.runtime.persistence;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HybridMigrationClusterStateTest.TestContext.class)
public class HybridMigrationClusterStateTest {
    @Autowired
    private HybridMigrationClusterState migrationState;

    @Test
    public void testMigrationState() {
        // Claiming it the first time should work

        assertTrue(migrationState.tryClaim(Object.class, "123"));

        // Claiming it a second time should not

        assertFalse(migrationState.tryClaim(Object.class, "123"));
    }

    @Configuration
    @Import(HybridMigrationClusterState.class)
    static class TestContext {
        @Bean
        public HazelcastInstance hazelcastInstance() {
            Config config = new Config();

            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

            return Hazelcast.newHazelcastInstance(config);
        }
    }
}