package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.google.common.collect.ImmutableList;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SharedBrainTest.TestContext.class)
public class SharedBrainTest {
    public static final String MAIL_ADDRESS = "test@example.com";

    @Autowired
    private SharedBrain sharedBrain;

    private EventStreamProcessor eventStreamProcessor;

    private ITopic<String> topic;

    @Before
    public void setUp() throws Exception {
        eventStreamProcessor = new EventStreamProcessor("testProcessor_" + UUID.randomUUID().toString(),
                ImmutableList.of(new Quota(1, 1, TimeUnit.SECONDS, 100, 2, TimeUnit.SECONDS)));
        topic = sharedBrain.createTopic("test", eventStreamProcessor);
    }

    @Test
    public void remembersViolationsWithinWindow() throws Exception {
        sharedBrain.rememberViolation(MAIL_ADDRESS, 100, "description", 2);

        TimeUnit.SECONDS.sleep(1);

        assertEquals(new QuotaViolationRecord(100, "description"), sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));

        TimeUnit.SECONDS.sleep(2);

        assertNull(sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));
    }

    @Configuration
    @Import(SharedBrain.class)
    static class TestContext {
        @Bean(destroyMethod = "shutdown")
        public HazelcastInstance hazelcastInstance() {
            return Hazelcast.newHazelcastInstance();
        }
    }
}