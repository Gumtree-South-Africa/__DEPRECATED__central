package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.base.Function;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static com.ecg.replyts.core.runtime.TimingReports.*;

class ChunkedIndexerAction implements IndexerAction {

    private static final Logger LOG = LoggerFactory.getLogger(ChunkedIndexerAction.class);
    // Setup here a quite long time before job will be stopped because full index will also done with this indexer and might take some time.
    private static final int MAX_PROCESSING_TIME_DAYS = 20;

    private Counter submittedConvCounter = newCounter("indexer.chunked-submittedConvCounter");
    private final Timer TIMER = TimingReports.newTimer("indexer.chunked-indexConversations");

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
        LOG.debug("Full indexing complete, indexed {} conversations", submittedConvCounter.getCount());
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
            try (Timer.Context ignored = TIMER.time()) {
                List<String> conversationIds = conversationRepository.listConversationsModifiedBetween(slice.lowerEndpoint(), slice.upperEndpoint());
                if (!conversationIds.isEmpty()) {
                    submittedConvCounter.inc(conversationIds.size());
                    LOG.debug("Indexing conversations from {}, to {}", slice.lowerEndpoint(), slice.upperEndpoint());
                    LOG.trace("Indexing conversation ids: {}", conversationIds.toString());
                    LOG.debug("Indexing {} conversation, total indexed so far {}", conversationIds.size(), submittedConvCounter.getCount());
                    journal.completedChunk();
                    indexerChunkHandler.indexChunk(conversationIds);
                }
            }
        }
    }
}