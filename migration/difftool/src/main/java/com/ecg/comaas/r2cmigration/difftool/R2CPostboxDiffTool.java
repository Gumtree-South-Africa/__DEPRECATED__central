package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
import com.ecg.comaas.r2cmigration.difftool.repo.CassPostboxRepo;
import com.ecg.comaas.r2cmigration.difftool.repo.RiakPostboxRepo;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

@Service
public class R2CPostboxDiffTool {


    private static final Logger LOG = LoggerFactory.getLogger(R2CPostboxDiffTool.class);
    private static final Logger MISMATCH_LOG = LoggerFactory.getLogger("difftool-postbox.mismatch");

    final static Counter RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("difftool.riak-postbox-mismatch-counter");
    final static Counter CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER = TimingReports.newCounter("difftool.cass-postbox-mismatch-counter");

    Counter cassPostboxCounter;
    Counter riakPostboxCounter;
    Counter emptyRiakPostboxCounter;

    volatile boolean isRiakMatchesCassandra = true;
    volatile boolean isCassandraMatchesRiak = true;
    private int idBatchSize;
    private int maxEntityAge;
    private DateTime endDate;
    private DateTime startDate;
    private int tzShiftInMin;
    private boolean verbose;

    @Autowired
    RiakPostboxRepo riakRepo;

    @Autowired
    CassPostboxRepo cassRepo;

    @Autowired
    ThreadPoolExecutor executor;

    public R2CPostboxDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        LOG.info("Max TTL {} days", maxEntityAge);

        cassPostboxCounter = newCounter("difftool.cassPostboxCounter");
        riakPostboxCounter = newCounter("difftool.riakPostboxCounter");
        emptyRiakPostboxCounter = newCounter("difftool.emptyRiakPostboxCounter");
    }

    public void setDateRange(DateTime startDate, DateTime endDate, int tzShiftInMin) {
        this.tzShiftInMin = tzShiftInMin;
        if (endDate != null) {
            this.endDate = endDate;
        } else {
            this.endDate = new DateTime(DateTimeZone.UTC);
        }
        if (startDate != null) {
            this.startDate = startDate;
        } else {
            this.startDate = this.endDate.minusDays(maxEntityAge);
        }
        Preconditions.checkArgument(this.endDate.isBeforeNow());
        Preconditions.checkArgument(this.startDate.isBefore(this.endDate));
        if (startDate != null) {
            LOG.info("Compare between {} and {}", this.endDate, this.startDate);
        } else {
            LOG.info("Comparing last {} days, starting from {}", maxEntityAge, this.startDate);
        }
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
                    Future task = executor.submit(() -> {

                        pboxidBatch.forEach(pboxId -> {
                            compareAndLogTheDiff(pboxId.getObjectKey(), true);
                        });

                    });
                    tasks.add(task);
                });
        return tasks;
    }

    void compareAndLogTheDiff(String pboxId, boolean riakToCassandra) {

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

    private void getMismatchedMessages(PostBox riakpbox, PostBox casspbox) {
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
                            boolean eq = riakAc.equals(cassAc);
                            if (!eq) {
                                LOG.debug("AbstractConv are different: riak {}\n cass {}", riakAc, cassAc);
                            }
                            return !eq;
                        } else {

                            return true;
                        }
                    })
                    // This prints out the differences
                    .forEach(i -> {
                        LOG.debug("Printing out the differences! ");
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

    public List<Future> compareCassToRiakAsync() throws RiakException {
        DateTime tzStart = startDate.plusMinutes(tzShiftInMin);
        DateTime tzEnd = endDate.plusMinutes(tzShiftInMin);
        LOG.info("Comparing Cassandra postboxes to Riak within the data range {} - {}, timezone correction is {} min", tzStart, tzEnd, tzShiftInMin);

        Stream<String> postboxIds = cassRepo.streamMessageBoxIds(tzStart.toDate(), tzEnd.toDate());
        List<Future> tasks = new ArrayList<>();
        Iterators.partition(postboxIds.iterator(), idBatchSize).forEachRemaining(
                pboxidBatch -> {

                    cassPostboxCounter.inc(pboxidBatch.size());
                    Future task = executor.submit(() -> {

                        pboxidBatch.forEach(pboxId -> {
                            compareAndLogTheDiff(pboxId, false);
                        });
                    });
                    tasks.add(task);
                });

        return tasks;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
