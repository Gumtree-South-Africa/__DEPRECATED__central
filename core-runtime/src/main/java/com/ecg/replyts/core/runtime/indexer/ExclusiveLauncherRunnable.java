package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.runtime.DateSliceIterator.IterationDirection;
import org.joda.time.DateTime;

class ExclusiveLauncherRunnable implements Runnable {

    private final IndexingMode mode;
    private final DateTime dateFrom;
    private final IndexerClockRepository indexerClockRepository;
    private final IndexingJournals indexingJournals;
    private final IndexerAction indexerAction;

    ExclusiveLauncherRunnable(IndexingMode mode, DateTime dateFrom, IndexerClockRepository indexerClockRepository, IndexingJournals indexingJournals, IndexerAction indexerAction) {
        this.mode = mode;
        this.dateFrom = dateFrom;
        this.indexerClockRepository = indexerClockRepository;
        this.indexingJournals = indexingJournals;
        this.indexerAction = indexerAction;
    }

    @Override
    public void run() {
        DateTime executionStartDate = DateTime.now();
        IterationDirection indexingDirection = mode.indexingDirection();

        if (indexingDirection == IterationDirection.PRESENT_TO_PAST) {
            indexerClockRepository.set(executionStartDate);
        }

        IndexingJournal journal = indexingJournals.createJournalFor(dateFrom, executionStartDate, mode);
        try {
            indexerAction.doIndexBetween(dateFrom, executionStartDate, mode, journal);
        } finally {
            journal.completeRunning();
        }

        if (indexingDirection == IterationDirection.PAST_TO_PRESENT) {
            indexerClockRepository.set(executionStartDate);
        }
    }
}