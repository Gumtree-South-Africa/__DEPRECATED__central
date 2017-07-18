package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.google.common.collect.ImmutableList;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedBrainTest {
    public static final String MAIL_ADDRESS = "test@example.com";

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Mock
    private Config hazelcastConfig;

    @Mock
    private ITopic communicationBus;

    @Mock
    private IMap violationMemoryMap;

    private SharedBrain sharedBrain;

    private EventStreamProcessor eventStreamProcessor;

    @Before
    public void setUp() throws Exception {
        MapConfig mapConfig = mock(MapConfig.class);

        when(hazelcastInstance.getTopic(anyString())).thenReturn(communicationBus);
        when(hazelcastInstance.getConfig()).thenReturn(hazelcastConfig);
        when(hazelcastConfig.getMapConfig(anyString())).thenReturn(mapConfig);

        when(hazelcastInstance.getMap(anyString())).thenReturn(violationMemoryMap);

        eventStreamProcessor = new EventStreamProcessor("testProcessor_" + UUID.randomUUID().toString(),
                ImmutableList.of(new Quota(1, 1, TimeUnit.SECONDS, 100, 2, TimeUnit.SECONDS)));
        sharedBrain = new SharedBrain("test", hazelcastInstance, eventStreamProcessor);
    }

    @Test
    public void remembersViolationsWithinWindow() throws Exception {
        QuotaViolationRecord expected = new QuotaViolationRecord(100, "description");

        ICompletableFuture<QuotaViolationRecord> putResult = mock(ICompletableFuture.class);
        when(putResult.get(anyLong(), any(TimeUnit.class))).thenReturn(null);

        when(violationMemoryMap.putAsync(anyString(), any(QuotaViolationRecord.class), anyLong(), any(TimeUnit.class))).thenReturn(putResult);

        ICompletableFuture<QuotaViolationRecord> getResult = mock(ICompletableFuture.class);
        when(getResult.get(anyLong(), any(TimeUnit.class))).thenReturn(expected, null); // First return expected result; then null

        when(violationMemoryMap.getAsync(anyString())).thenReturn(getResult);

        sharedBrain.rememberViolation(MAIL_ADDRESS, 100, "description", 2);

        assertEquals(new QuotaViolationRecord(100, "description"), sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));

        assertNull(sharedBrain.getViolationRecordFromMemory(MAIL_ADDRESS));
    }
}
