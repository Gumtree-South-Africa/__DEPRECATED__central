package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchIndexerTest {
    @Mock
    private IndexerClockRepository indexerClockRepository;

    @Mock
    private IndexerHealthCheck healthCheck;

    @Mock
    private SingleRunGuard singleRunGuard;

    @Mock
    private IndexerAction indexerAction;

    @Mock
    private IndexingJournals indexingJournals;

    @InjectMocks
    private ElasticSearchIndexer indexer;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(indexer, "maxAgeDays", 1); // Will ensure startTimeForFullIndex() returns now() - 1 day
        when(indexerClockRepository.get()).thenReturn(DateTime.now());
    }

    @Test
    public void runFullIndexReportingOkay() {
        indexer.fullIndex();

        verify(healthCheck, times(2)).reportFull(eq(Status.OK), any(Message.class));
    }

    @Test
    public void runFullIndexReportingFailed() {
        when(singleRunGuard.runExclusivelyOrSkip(any(IndexingMode.class), any(Runnable.class))).thenThrow(new RuntimeException());
        try {
            indexer.fullIndex();
            fail();
        } catch (RuntimeException re) {
            // expected
        }
        verify(healthCheck).reportFull(eq(Status.OK), any(Message.class));
        verify(healthCheck).reportFull(eq(Status.CRITICAL), any(Message.class));
    }

    @Test
    public void runSingleGuardWhenFullIndex() {
        indexer.fullIndex();

        verify(singleRunGuard).runExclusivelyOrSkip(eq(IndexingMode.FULL), any(ExclusiveLauncherRunnable.class));
    }

    @Test
    public void runDeltaIndexReportingOkay() {
        indexer.deltaIndex();

        verify(healthCheck, times(2)).reportDelta(eq(Status.OK), any(Message.class));
    }

    @Test
    public void runDeltaIndexReportingFailed() {
        when(singleRunGuard.runExclusivelyOrSkip(any(IndexingMode.class), any(Runnable.class))).thenThrow(new RuntimeException());
        try {
            indexer.deltaIndex();
            fail();
        } catch (RuntimeException re) {
            // expected
        }
        verify(healthCheck).reportDelta(eq(Status.OK), any(Message.class));
        verify(healthCheck).reportDelta(eq(Status.CRITICAL), any(Message.class));
    }

    @Test
    public void runSingleGuardWhenDeltaIndex() {
        indexer.deltaIndex();

        verify(indexerClockRepository).get();
        verify(singleRunGuard).runExclusivelyOrSkip(eq(IndexingMode.DELTA), any(ExclusiveLauncherRunnable.class));
    }

    @Test
    public void checkFullIndexDateIfNoLastrun() {
        when(indexerClockRepository.get()).thenReturn(null);

        indexer.deltaIndex();

        verify(singleRunGuard).runExclusivelyOrSkip(eq(IndexingMode.DELTA), any(ExclusiveLauncherRunnable.class));
    }
}
