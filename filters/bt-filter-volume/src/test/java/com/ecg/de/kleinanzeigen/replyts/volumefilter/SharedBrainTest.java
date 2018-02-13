package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.google.common.collect.ImmutableList;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SharedBrainTest {
    public static final String MAIL_ADDRESS = "test@example.com";

    private HazelcastInstance hazelcastInstance;

    private EventStreamProcessor eventStreamProcessor;

    private SharedBrain sharedBrain;

    @Before
    public void setUp() throws Exception {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        eventStreamProcessor = new EventStreamProcessor("testProcessor_" + UUID.randomUUID().toString(),
                ImmutableList.of(new Quota(1, 1, TimeUnit.SECONDS, 100, 2, TimeUnit.SECONDS)));
        sharedBrain = new SharedBrain("test", hazelcastInstance, eventStreamProcessor);
    }

    @After
    public void tearDown() throws Exception {
        hazelcastInstance.shutdown();
    }

    @Test
    @Ignore // Ignore this test for now, as Hazelcast instantiation is broken after upgrading to a new version
    public void remembersViolationsWithinWindow() throws Exception {
        sharedBrain.rememberViolation(MAIL_ADDRESS, 100, "description", 2);
        TimeUnit.SECONDS.sleep(1);

        assertEquals(new QuotaViolationRecord(100, "description"), sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));

        TimeUnit.SECONDS.sleep(2);

        assertNull(sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));
    }
}