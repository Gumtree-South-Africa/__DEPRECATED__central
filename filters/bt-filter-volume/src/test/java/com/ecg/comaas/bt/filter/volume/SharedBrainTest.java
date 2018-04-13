package com.ecg.comaas.bt.filter.volume;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SharedBrainTest.TestContext.class)
public class SharedBrainTest {
    public static final String MAIL_ADDRESS = "test@example.com";

    @Autowired
    private SharedBrain sharedBrain;

    @Autowired
    private IMap violationMemoryMap;

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

    @Configuration
    @Import(SharedBrain.class)
    static class TestContext {
        @MockBean
        private IMap violationMemoryMap;

        @Bean
        public HazelcastInstance hazelcastInstance(IMap violationMemoryMap) {
            HazelcastInstance instance = mock(HazelcastInstance.class);

            when(instance.getConfig()).thenReturn(mock(Config.class));
            when(instance.getMap(anyString())).thenReturn(violationMemoryMap);

            return instance;
        };
    }
}