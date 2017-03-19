package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.repo.CassPostboxRepo;
import com.ecg.comaas.r2cmigration.difftool.repo.RiakPostboxRepo;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

public class R2CPostboxDiffTool extends AbstractDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(R2CPostboxDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("postbox.mismatch");

    final static Counter RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("riak-postbox-mismatch-counter");
    final static Counter CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("cass-postbox-mismatch-counter");

    private final static Timer RIAK_TO_CASS_BATCH_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.postbox.batch-compare-timer");
    private final static Timer CASS_TO_RIAK_BATCH_COMPARE_TIMER = TimingReports.newTimer("cass-to-riak.postbox.batch-compare-timer");

    private final static Timer RIAK_TO_CASS_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.postbox.compare-timer");
    private final static Timer CASS_TO_RIAK_COMPARE_TIMER = TimingReports.newTimer("cass-to-riak.postbox.compare-timer");
    private final static Timer EXTRACT_MISMATCHES_TIMER = TimingReports.newTimer("postbox.extract-mismatches");

    Counter cassPostboxCounter;
    Counter riakPostboxCounter;
    Counter emptyRiakPostboxCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;
    private int idBatchSize;
    private boolean verbose;

    @Autowired
    RiakPostboxRepo riakRepo;

    @Autowired
    CassPostboxRepo cassRepo;

    @Autowired
    ExecutorService executor;

    public R2CPostboxDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        LOG.info("Max TTL {} days", maxEntityAge);

        cassPostboxCounter = newCounter("cassPostboxCounter");
        riakPostboxCounter = newCounter("riakPostboxCounter");
        emptyRiakPostboxCounter = newCounter("emptyRiakPostboxCounter");
    }

    public long getMessagesCountInTimeSlice(boolean riakToCass) throws RiakException {
        if (riakToCass) {
            return riakRepo.getMessagesCount(startDate, endDate);
        } else {
            DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
            DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
            LOG.info("Streaming Cassandra postboxes between startDate {} endDate {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);
            return cassRepo.getPostboxModificationCountByQuery(tzStart.toDate(), tzEnd.toDate());
        }
    }

    public List<Future> compareRiakToCassAsync() throws RiakException {
        LOG.info("Comparing Riak postboxes to Cassandra within the data range {} - {} )", startDate, endDate);
        StreamingOperation<IndexEntry> postboxIds = riakRepo.streamPostBoxIds(startDate, endDate);
        List<Future> tasks = new ArrayList<>(idBatchSize * 3);
        Iterators.partition(postboxIds.iterator(), idBatchSize).forEachRemaining(
                pboxidBatch -> {

                    riakPostboxCounter.inc(pboxidBatch.size());
                    Future task = executor.submit(() -> compareBatchRiakToCassAsync(pboxidBatch));
                    tasks.add(task);
                });
        return tasks;
    }

    private void compareBatchRiakToCassAsync(List<IndexEntry> pboxidBatch) {
        try (Timer.Context ignored = RIAK_TO_CASS_BATCH_COMPARE_TIMER.time()) {
            pboxidBatch.forEach(pboxId -> {
                compareAndLogTheDiff(pboxId.getObjectKey(), true);
            });
        }
    }

    void compareAndLogTheDiff(String pboxId, boolean riakToCassandra) {
        Timer timer = riakToCassandra ? RIAK_TO_CASS_COMPARE_TIMER : CASS_TO_RIAK_COMPARE_TIMER;
        try (Timer.Context ignored = timer.time()) {

            PostBox riakPbox = riakRepo.getById(pboxId);
            // We are skipping empty riak postboxes, no point in comparing.
            if (riakToCassandra && (riakPbox.getConversationThreads().isEmpty())) {
                emptyRiakPostboxCounter.inc();
                return;
            }

            PostBox cassPbox = cassRepo.getById(pboxId);

            LOG.debug("Comparing postboxes for id '{}' ", pboxId);
            boolean mismatch = false;
            // First see if obvious things match
            if (!riakPbox.getEmail().equalsIgnoreCase(cassPbox.getEmail()) || riakPbox.getConversationThreads().size() != cassPbox.getConversationThreads().size())
                mismatch = true;
            else if (!riakPbox.equals(cassPbox)) {
                mismatch = true;
            }

            if (mismatch) {
                // Check if the messages are new, ignore the difference in such case

                getMismatchedMessages(riakPbox, cassPbox);

                ArrayList<? extends AbstractConversationThread> riakConv = new ArrayList<>(riakPbox.getConversationThreads());
                ArrayList<? extends AbstractConversationThread> cassConv = new ArrayList<>(cassPbox.getConversationThreads());

                // When comparing stale source like cassandra to "live" source like riak, expect new conversations in riak
                if (cassConv.size() < riakConv.size()) {
                    int removedCount = 0;
                    for (int i = 0; i < cassConv.size(); i++) {
                        // These are sorted
                        AbstractConversationThread riakCt = riakConv.get(i);
                        AbstractConversationThread cassCt = cassConv.get(i);
                        if (riakCt.equals(cassCt)) {
                            riakConv.remove(cassCt);
                            removedCount++;
                        }
                    }

                    // Assume matches to all cass conv were found in riak, do not count as mismatch
                    if (removedCount == cassConv.size()) {
                        LOG.info("Riak postbox was updated, conversation missing from cassandra are: \n{}", riakConv);
                        return;
                    }
                }

                LOG.error("Postbox id '{}' do not match that of {}!", pboxId, riakToCassandra ? "riak" : "cass");
                LOG.info("Riak postbox: {}", verbose ? riakPbox.fullToString() : riakPbox.toString());
                LOG.info("Cass postbox: {}\n", verbose ? cassPbox.fullToString() : cassPbox.toString());

                MISMATCH_LOG.info(pboxId);

                if (riakToCassandra) {
                    RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER.inc();
                    isRiakMatchesCassandra = false;
                } else {
                    CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER.inc();
                    isCassandraMatchesRiak = false;
                }

            }
        }
    }

    private void getMismatchedMessages(PostBox riakpbox, PostBox casspbox) {
        try (Timer.Context ignored = EXTRACT_MISMATCHES_TIMER.time()) {

            synchronized (riakpbox) {

                if (riakpbox.getNewRepliesCounter() != casspbox.getNewRepliesCounter()) {
                    MISMATCH_LOG.debug("NewRepliesCounter riak {}, cass {}", riakpbox.getNewRepliesCounter().getValue(), casspbox.getNewRepliesCounter().getValue());
                }

                List<AbstractConversationThread> riakCt = new ArrayList<>(riakpbox.getConversationThreads());
                List<AbstractConversationThread> cassCt = new ArrayList<>(casspbox.getConversationThreads());

                Collections.sort(riakCt, PostBox.RECEIVED_MODIFIED_CREATION_DATE.reversed());
                Collections.sort(cassCt, PostBox.RECEIVED_MODIFIED_CREATION_DATE.reversed());

                MISMATCH_LOG.debug("Number of conversations riak {}, cass {}", riakCt.size(), cassCt.size());
                int lastIdx = riakCt.size() > cassCt.size() ? riakCt.size() : cassCt.size();

                IntStream.range(0, lastIdx)
                        // This filters out equal conversations
                        .filter(i -> {
                            if (riakCt.size() > i && cassCt.size() > i) {

                                AbstractConversationThread riakAc = riakCt.get(i);
                                AbstractConversationThread cassAc = cassCt.get(i);
                                if (!riakAc.equals(cassAc)) {
                                    LOG.debug("AbstractConv are different: riak {}\n cass {}", riakAc, cassAc);
                                }
                                return false;
                            } else {

                                return true;
                            }
                        })
                        // This prints out the differences
                        .forEach(i -> {
                            if (cassCt.size() > i && riakCt.size() > i) {

                                MISMATCH_LOG.debug("Message mismatching  \nriak: {} \ncass: {} ",
                                        verbose ? riakCt.get(i).fullToString() : riakCt.get(i),
                                        verbose ? cassCt.get(i).fullToString() : cassCt.get(i));

                            } else if (cassCt.size() > i) {

                                MISMATCH_LOG.debug("Message missing from riak but present in cassandra \n{} ", cassCt.get(i));
                            } else if (riakCt.size() > i) {

                                MISMATCH_LOG.debug("Message missing from cass but present in riak \n{} ", riakCt.get(i));
                            } else {

                                throw new RuntimeException("Filtering issue in getMismatchedMessages!");
                            }
                        });
            }
        }
    }

    public List<Future> compareCassToRiakAsync() throws RiakException {
        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Comparing Cassandra postboxes to Riak within the data range {} - {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);

        Stream<String> postboxIds = cassRepo.streamMessageBoxIds(tzStart.toDate(), tzEnd.toDate());
        List<Future> tasks = new ArrayList<>();
        Iterators.partition(postboxIds.iterator(), idBatchSize).forEachRemaining(
                pboxidBatch -> {

                    cassPostboxCounter.inc(pboxidBatch.size());
                    Future task = executor.submit(() -> compareBatchCassToRiakAsync(pboxidBatch));
                    tasks.add(task);
                });

        return tasks;
    }

    private void compareBatchCassToRiakAsync(List<String> pboxidBatch) {
        try (Timer.Context ignored = CASS_TO_RIAK_BATCH_COMPARE_TIMER.time()) {
            pboxidBatch.forEach(pboxId -> {
                compareAndLogTheDiff(pboxId, false);
            });
        }
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
