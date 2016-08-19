package com.ecg.replyts.core.runtime.indexer;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SingleRunGuardTest {
    @Mock
    HazelcastInstance hazelcastInstance;

    @Mock
    IndexingMode indexingMode;

    @Mock
    Runnable runnable;

    @Mock
    ILock lock;

    @InjectMocks
    private SingleRunGuard singleRunGuard;

    @Test
    public void shouldNotCallUnlockIfLockDoesNotBelongToCurrentThread() throws InterruptedException {
        when(hazelcastInstance.getLock(anyString())).thenReturn(lock);
        when(lock.isLockedByCurrentThread()).thenReturn(false);
        when(lock.tryLock(anyLong(), any())).thenReturn(false);

        singleRunGuard.runExclusivelyOrSkip(indexingMode, runnable);

        Mockito.verify(lock, never()).unlock();
    }
}
