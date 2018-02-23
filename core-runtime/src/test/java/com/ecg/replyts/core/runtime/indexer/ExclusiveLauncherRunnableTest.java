package com.ecg.replyts.core.runtime.indexer;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExclusiveLauncherRunnableTest {

    @Mock
    private IndexerClockRepository indexerClockRepository;

    @Mock
    private IndexerAction indexerAction;

    @Mock
    private IndexingJournals indexingJournals;

    @Mock
    private IndexingJournal journal;

    @Before
    public void setUp() {
        when(indexingJournals.createJournalFor(any(DateTime.class), any(DateTime.class), any(IndexingMode.class))).thenReturn(journal);
    }

    @Test
    public void createNewJournalForIndex() {
        run(IndexingMode.FULL);

        InOrder inOrder = inOrder(indexingJournals, journal);

        inOrder.verify(indexingJournals).createJournalFor(any(DateTime.class), any(DateTime.class), eq(IndexingMode.FULL));
        inOrder.verify(journal).completeRunning();
    }

    @Test
    public void setsClockFirstOnFullIndex() {
        run(IndexingMode.FULL);

        InOrder inOrder = inOrder(indexerClockRepository, indexerAction);
        inOrder.verify(indexerClockRepository).set(any(DateTime.class));
        inOrder.verify(indexerAction).doIndexBetween(any(DateTime.class), any(DateTime.class), eq(IndexingMode.FULL), eq(journal));
    }

    @Test
    public void setsClockLastOnDeltaIndex() {
        run(IndexingMode.DELTA);

        InOrder inOrder = inOrder(indexerClockRepository, indexerAction);
        inOrder.verify(indexerAction).doIndexBetween(any(DateTime.class), any(DateTime.class), eq(IndexingMode.DELTA), eq(journal));
        inOrder.verify(indexerClockRepository).set(any(DateTime.class));
    }

    private void run(IndexingMode indexingMode) {
        new ExclusiveLauncherRunnable(indexingMode, DateTime.now().minusDays(1), DateTime.now(), indexerClockRepository, indexingJournals, indexerAction).run();
    }
}
