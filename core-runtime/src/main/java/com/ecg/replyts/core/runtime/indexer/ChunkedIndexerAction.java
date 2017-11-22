package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid') }")
public class ChunkedIndexerAction implements IndexerAction {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkedIndexerAction.class);

    private static final Counter SUBMITTED_CONVERSATIONS_COUNTER = newCounter("indexer.chunked-submittedConvCounter");
    private static final Timer INDEX_CONVERSATIONS_TIMER = TimingReports.newTimer("indexer.chunked-indexConversations");

    private static final int MAX_PROCESSING_TIME_DAYS = 20; // Needed as this will also be used for a full reindex

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private IndexerChunkHandler indexerChunkHandler;

    @Value("${batch.bulkoperations.threadcount:4}")
    private int threadCount;

    @Value("${batch.indexer.chunksize.minutes:10}")
    private int chunkSizeMinutes;

    @Override
    public void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal) {
        DateSliceIterator dateSlices = new DateSliceIterator(Range.closed(dateFrom, dateTo), chunkSizeMinutes, MINUTES, indexingMode.indexingDirection());
        journal.startRunning(dateSlices.chunkCount());

        BlockingBatchExecutor<Range<DateTime>> executor = new BlockingBatchExecutor<>("indexing-" + indexingMode.name(), threadCount, MAX_PROCESSING_TIME_DAYS, DAYS);

        executor.executeAll(dateSlices, slice -> new IndexChunkRunnable(slice, journal), indexingMode.errorHandlingPolicy());
        LOG.debug("Full indexing complete, indexed {} conversations", SUBMITTED_CONVERSATIONS_COUNTER.getCount());
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
            try (Timer.Context ignored = INDEX_CONVERSATIONS_TIMER.time()) {
                List<String> conversationIds = conversationRepository.listConversationsModifiedBetween(slice.lowerEndpoint(), slice.upperEndpoint());
                if (!conversationIds.isEmpty()) {
                    SUBMITTED_CONVERSATIONS_COUNTER.inc(conversationIds.size());

                    LOG.debug("Indexing conversations from {}, to {}", slice.lowerEndpoint(), slice.upperEndpoint());
                    LOG.trace("Indexing conversation ids: {}", conversationIds.toString());
                    LOG.debug("Indexing {} conversation, total indexed so far {}", conversationIds.size(), SUBMITTED_CONVERSATIONS_COUNTER.getCount());

                    journal.completedChunk();
                    indexerChunkHandler.indexChunk(conversationIds);
                }
            }
        }
    }
}