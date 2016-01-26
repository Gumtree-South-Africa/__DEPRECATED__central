package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.api.indexer.IndexerStatus;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.ecg.replyts.core.api.util.CurrentClock;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Optional.fromNullable;

class ElasticSearchIndexer implements Indexer {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    private final IndexerClockRepository indexerClockRepository;
    private final IndexerHealthCheck healthCheck;
    private final SingleRunGuard singleRunGuard;
    private final IndexerAction indexerAction;
    private final IndexingJournals indexingJournals;
    private final IndexStartPoint indexStartPoint;

    @Autowired
    ElasticSearchIndexer(
            IndexerHealthCheck healthCheck,
            @Value("${replyts.maxConversationAgeDays}") int maxAgeDays,
            IndexerClockRepository indexerClockRepository,
            SingleRunGuard singleRunGuard,
            IndexerAction indexerAction,
            IndexingJournals indexingJournals) {

        this(healthCheck, indexerClockRepository, singleRunGuard, indexerAction, indexingJournals, new IndexStartPoint(new CurrentClock(), maxAgeDays));
    }

    ElasticSearchIndexer(
            IndexerHealthCheck healthCheck,
            IndexerClockRepository indexerClockRepository,
            SingleRunGuard singleRunGuard,
            IndexerAction indexerAction,
            IndexingJournals indexingJournals,
            IndexStartPoint indexStartPoint) {

        this.indexerClockRepository = indexerClockRepository;
        this.healthCheck = healthCheck;

        this.singleRunGuard = singleRunGuard;
        this.indexerAction = indexerAction;
        this.indexingJournals = indexingJournals;
        this.indexStartPoint = indexStartPoint;
    }

    @Override
    public void fullIndex() {
        healthCheck.reportFull(Status.OK, Message.shortInfo("Full Indexing started at " + new DateTime()));
        try {
            doIndexFromDate(indexStartPoint.startTimeForFullIndex(), IndexingMode.FULL);
            healthCheck.reportFull(Status.OK, Message.shortInfo("Full Indexing finished at " + new DateTime()));
        } catch (RuntimeException e) {
            healthCheck.reportFull(Status.CRITICAL, Message.fromException("Full Indexing failed", e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deltaIndex() {
        healthCheck.reportDelta(Status.OK, Message.shortInfo("Delta Indexing started at " + new DateTime()));
        try {
            DateTime dateFrom = fromNullable(indexerClockRepository.get()).or(indexStartPoint.startTimeForFullIndex());

            doIndexFromDate(dateFrom, IndexingMode.DELTA);
            healthCheck.reportDelta(Status.OK, Message.shortInfo("Delta Indexing finished at " + new DateTime()));
        } catch (RuntimeException e) {
            healthCheck.reportDelta(Status.CRITICAL, Message.fromException("Delta Indexing failed", e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public void indexSince(DateTime since) {
        doIndexFromDate(since, IndexingMode.FULL);
    }

    private void doIndexFromDate(DateTime dateFrom, IndexingMode mode) {
        ExclusiveLauncherRunnable indexLauncher = new ExclusiveLauncherRunnable(mode, dateFrom, indexerClockRepository, indexingJournals, indexerAction);
        boolean jobExecuted = singleRunGuard.runExclusivelyOrSkip(mode, indexLauncher);

        if (!jobExecuted && mode == IndexingMode.FULL) {
            LOG.error("Skipped Full Indexing: another process was doing that already");
        }
    }

    @Override
    public List<IndexerStatus> getStatus() {
        return Arrays.asList(
                indexingJournals.getLastRunStatisticsFor(IndexingMode.DELTA),
                indexingJournals.getLastRunStatisticsFor(IndexingMode.FULL)
        );
    }
}