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
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTimeZone.UTC;

@Component
public class ElasticSearchIndexer implements Indexer {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    @Autowired
    private IndexerClockRepository indexerClockRepository;

    @Autowired
    private IndexerHealthCheck healthCheck;

    @Autowired
    private SingleRunGuard singleRunGuard;

    @Autowired
    private IndexerAction indexerAction;

    @Autowired
    private IndexingJournals indexingJournals;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    private CurrentClock clock = new CurrentClock();

    @Override
    public void fullIndex() {
        String startMsg = "Full Indexing started at " + new DateTime().withZone(UTC);
        LOG.debug(startMsg);
        healthCheck.reportFull(Status.OK, Message.shortInfo(startMsg));

        try {
            doIndexFromDate(startTimeForFullIndex(), IndexingMode.FULL);

            String endMsg = "Full Indexing finished at " + new DateTime().withZone(UTC);
            LOG.debug(endMsg);
            healthCheck.reportFull(Status.OK, Message.shortInfo(endMsg));
        } catch (RuntimeException e) {
            healthCheck.reportFull(Status.CRITICAL, Message.fromException("Full Indexing failed", e));
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deltaIndex() {
        String startMsg = "Delta Indexing started at " + new DateTime().withZone(UTC);
        LOG.debug(startMsg);
        healthCheck.reportDelta(Status.OK, Message.shortInfo(startMsg));

        try {
            DateTime dateFrom = Optional.ofNullable(indexerClockRepository.get()).orElse(startTimeForFullIndex());

            doIndexFromDate(dateFrom, IndexingMode.DELTA);

            String endMsg = "Delta Indexing finished at " + new DateTime().withZone(UTC);
            LOG.debug(endMsg);
            healthCheck.reportDelta(Status.OK, Message.shortInfo(endMsg));
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
        return Arrays.asList(indexingJournals.getLastRunStatisticsFor(IndexingMode.DELTA), indexingJournals.getLastRunStatisticsFor(IndexingMode.FULL));
    }

    private DateTime startTimeForFullIndex() {
        return new DateTime(clock.now()).minusDays(maxAgeDays);
    }
}