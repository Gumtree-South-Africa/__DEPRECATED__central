package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
import com.ecg.comaas.r2cmigration.difftool.repo.CassPostboxRepo;
import com.ecg.comaas.r2cmigration.difftool.repo.RiakPostboxRepo;
import com.ecg.comaas.r2cmigration.difftool.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

@Service
public class R2CPostboxDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(R2CPostboxDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("difftool-postbox.mismatch");


    final static Counter RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("difftool.riak-postbox-mismatch-counter");
    final static Counter CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("difftool.cass-postbox-mismatch-counter");

    final private ArrayBlockingQueue<Runnable> workQueue;
    final private RejectedExecutionHandler rejectionHandler;
    final ExecutorService threadPoolExecutor;

    Counter cassPostboxCounter;
    Counter riakPostboxCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;

    private int postboxIdBatchSize;

    @Autowired
    RiakPostboxRepo riakRepo;

    @Autowired
    CassPostboxRepo cassRepo;

    private DateTime endDate;
    private DateTime startDate;


    public R2CPostboxDiffTool(@Value("${replyts.maxConversationAgeDays:180}") int compareNumberOfDays,
                              @Value("${threadcount:6}") int threadCount,
                              @Value("${queue.size:100}") int workQueueSize,
                              @Value("${mbox.batch.size:1000}") int postboxIdBatchSize) {
        this.postboxIdBatchSize = postboxIdBatchSize;
        this.endDate = new DateTime(DateTimeZone.UTC);
        this.startDate = endDate.minusDays(compareNumberOfDays);
        this.workQueue = new ArrayBlockingQueue<>(workQueueSize);
        this.rejectionHandler = new InstrumentedCallerRunsPolicy("difftool", "");
        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        LOG.info("Comparing last {} days", compareNumberOfDays);

        cassPostboxCounter = newCounter("difftool.cassPostboxCounter");
        riakPostboxCounter = newCounter("difftool.riakPostboxCounter");
    }

    public long getMessagesToBeMigratedCount() throws RiakException {
        return riakRepo.getMessagesCount(startDate, endDate);
    }


    public long getCassPostboxCount() throws RiakException {
        return cassRepo.getPostboxesCountByQuery(startDate.toDate(), endDate.toDate());
        // TODO compare return cassRepo.getPostboxesCount(startDate.toDate(), endDate.toDate());
    }

    public List<Future> compareRiakToCassAsync() throws RiakException {
        LOG.info("Comparing Riak postboxes to Cassandra within the data range {} - {} (rounded: {})", startDate, endDate);
        long messCount = riakRepo.getMessagesCount(startDate, endDate);
        LOG.info("Found {} messages in riak", messCount);
        StreamingOperation<IndexEntry> postboxIds = riakRepo.streamPostBoxIds(startDate, endDate);
        List<Future> tasks = new ArrayList<>();
        Iterators.partition(postboxIds.iterator(), postboxIdBatchSize).forEachRemaining(
                pboxidBatch -> {
                    pboxidBatch.forEach(mi -> {
                        riakPostboxCounter.inc();
                        Future task = threadPoolExecutor.submit(() -> {
                            compareRiakToCass(mi.getObjectKey());
                        });
                        tasks.add(task);
                    });
                });
        return tasks;
    }

    public void compareRiakToCass(String postboxId)  {
        riakPostboxCounter.inc();
        LOG.debug("Comparing postboxes for id {} ", postboxId);
        PostBox riakPbox = riakRepo.getById(postboxId);
        PostBox cassPbox = cassRepo.getById(postboxId);
        compareAndLogTheDiff(riakPbox, cassPbox, postboxId, true);
    }

    void compareAndLogTheDiff(PostBox cassPbox, PostBox riakPbox, String pboxId, boolean riakToCassandra) {
        if ((riakPbox == null && cassPbox != null) || !riakPbox.equals(cassPbox)) {
            LOG.error("Postbox id {} do not match!", pboxId);
            LOG.debug("Riak postbox: {}", riakPbox);
            LOG.debug("Cass postbox: {}\n", cassPbox);
            if (riakToCassandra) {
                RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER.inc();
                isRiakMatchesCassandra = false;
            } else {
                CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER.inc();
                isCassandraMatchesRiak = false;
            }
        }
    }

    public void compareCassToRiak(String pboxId) {
            LOG.info("Comparing postbox id {}", pboxId);
            PostBox cassPbox = cassRepo.getById(pboxId);
            PostBox riakPbox = riakRepo.getById(pboxId);
            compareAndLogTheDiff(riakPbox, cassPbox, pboxId, false);
    }

    public List<Future> compareCassToRiakAsync() throws RiakException {
        DateTime roundedToHour = endDate.hourOfDay().roundFloorCopy().toDateTime();
        // TODO figure out what to do with rounded time
        LOG.info("Comparing Cassandra postboxes to Riak within the data range {} - {} (rounded: {})", startDate, endDate, roundedToHour);

        long messCount = cassRepo.getPostboxesCount(startDate.toDate(), endDate.toDate());
        LOG.info("Found {} messages in cassandra", messCount);
        Stream<String> postboxIds = cassRepo.streamMessageBoxIdsByHour(startDate.toDate(), endDate.toDate());

        List<Future> tasks = new ArrayList<>();
        Iterators.partition(postboxIds.iterator(), postboxIdBatchSize).forEachRemaining(
                pboxidBatch -> {
                    pboxidBatch.forEach(mi -> {
                        cassPostboxCounter.inc();
                        Future task = threadPoolExecutor.submit(() -> {
                            compareCassToRiak(mi);
                        });
                        tasks.add(task);
                    });
                });
        return tasks;
    }
}
