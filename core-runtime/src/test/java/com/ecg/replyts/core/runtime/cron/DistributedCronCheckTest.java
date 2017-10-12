package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.app.cronjobs.RiakCleanupConversationCronJob;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DistributedCronCheckTest {
    private HazelcastInstance hazelcast;

    private DistributedCronCheck distributedCronCheck;

    @Before
    public void setUp() throws Exception {
        hazelcast = Hazelcast.newHazelcastInstance();
        distributedCronCheck = new DistributedCronCheck(RiakCleanupConversationCronJob.class, hazelcast);
    }

    @After
    public void tearDown() throws Exception {
        hazelcast.shutdown();
    }

    @Test
    public void gaugesContinueReportingAfterHazelcastException() throws Exception {
        assertFalse(distributedCronCheck.isRunning());

        // Shutting down the cluster is the easiest way to cause hz exceptions to be thrown
        // from the gauge.
        // Coincidentally, the cluster could shut down in production if the node can't join
        // the cluster within a few mins.
        hazelcast.shutdown();

        assertFalse(distributedCronCheck.isRunning());
    }
}
