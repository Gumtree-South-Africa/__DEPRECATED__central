package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.runtime.DateSliceIterator.IterationDirection;
import org.joda.time.DateTime;

class ExclusiveLauncherRunnable implements Runnable {

    private final IndexingMode mode;
    private final DateTime dateFrom;
    private final DateTime dateTo;
    private final IndexerClockRepository indexerClockRepository;
    private final IndexingJournals indexingJournals;
    private final IndexerAction indexerAction;

    ExclusiveLauncherRunnable(IndexingMode mode, DateTime dateFrom, DateTime dateTo, IndexerClockRepository indexerClockRepository, IndexingJournals indexingJournals, IndexerAction indexerAction) {
        this.mode = mode;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.indexerClockRepository = indexerClockRepository;
        this.indexingJournals = indexingJournals;
        this.indexerAction = indexerAction;
    }

    @Override
    public void run() {
        IterationDirection indexingDirection = mode.indexingDirection();
        if (indexingDirection == IterationDirection.PRESENT_TO_PAST) {
            indexerClockRepository.set(dateTo);
        }

        IndexingJournal journal = indexingJournals.createJournalFor(dateFrom, dateTo, mode);
        try {
            indexerAction.doIndexBetween(dateFrom, dateTo, mode, journal);
        } finally {
            journal.completeRunning();
        }

        if (indexingDirection == IterationDirection.PAST_TO_PRESENT) {
            indexerClockRepository.set(dateTo);
        }
    }
}