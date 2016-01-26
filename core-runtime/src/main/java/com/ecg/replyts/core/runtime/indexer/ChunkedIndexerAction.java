package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.base.Function;
import com.google.common.collect.Range;
import org.joda.time.DateTime;

import java.util.List;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

class ChunkedIndexerAction implements IndexerAction {

    // Setup here a quite long time before job will be stopped because full index will also done with this indexer and might take some time.
    private static final int MAX_PROCESSING_TIME_DAYS = 20;

    private final IndexerChunkHandler indexerChunkHandler;
    private final ConversationRepository conversationRepository;
    private final int threadCount;
    private final int chunkSizeMinutes;

    ChunkedIndexerAction(
            ConversationRepository conversationRepository,
            IndexerChunkHandler indexerChunkHandler,
            int threadCount,
            int chunkSizeMinutes
    ) {
        this.conversationRepository = conversationRepository;
        this.indexerChunkHandler = indexerChunkHandler;
        this.threadCount = threadCount;
        this.chunkSizeMinutes = chunkSizeMinutes;
    }

    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        DateSliceIterator dateSlices = new DateSliceIterator(Range.closed(dateFrom, dateTo), chunkSizeMinutes, MINUTES, indexingMode.indexingDirection());
        journal.startRunning(dateSlices.chunkCount());

        BlockingBatchExecutor<Range<DateTime>> executor = new BlockingBatchExecutor<>("indexing-" + indexingMode.name(), threadCount, MAX_PROCESSING_TIME_DAYS, DAYS);

        executor.executeAll(dateSlices, new Function<Range<DateTime>, Runnable>() {
            @Override
            public Runnable apply(Range<DateTime> slice) {
                return new IndexChunkRunnable(slice, journal);
            }
        }, indexingMode.errorHandlingPolicy());
    }

    private class IndexChunkRunnable implements Runnable {
        private final Range<DateTime> slice;
        private final IndexingJournal journal;

        private IndexChunkRunnable(Range<DateTime> slice, IndexingJournal journal) {
            this.slice = slice;
            this.journal = journal;
        }

        @Override
        public void run() {
            List<String> conversationIds = conversationRepository.listConversationsModifiedBetween(slice.lowerEndpoint(), slice.upperEndpoint());
            journal.completedChunk();
            indexerChunkHandler.indexChunk(conversationIds);
        }
    }
}