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

    @Autowired
    RiakPostboxRepo riakRepo;

    @Autowired
    CassPostboxRepo cassRepo;

    @Autowired
    ThreadPoolExecutor executor;

    public R2CPostboxDiffTool(int idBatchSize, int maxEntityAge) {
        this.idBatchSize = idBatchSize;
        this.maxEntityAge = maxEntityAge;
        LOG.info("Postbox max TTL {} days", maxEntityAge);

        cassPostboxCounter = newCounter("difftool.cassPostboxCounter");
        riakPostboxCounter = newCounter("difftool.riakPostboxCounter");
        emptyRiakPostboxCounter = newCounter("difftool.emptyRiakPostboxCounter");
    }

    public void setDateRange(DateTime startDate, DateTime endDate) {
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
            LOG.info("Comparing last {} days, ending on {}", maxEntityAge, this.endDate);
        }
    }

    public long getMessagesToBeMigratedCount() throws RiakException {
        return riakRepo.getMessagesCount(startDate, endDate);
    }

    public List<Future> compareRiakToCassAsync() throws RiakException {
        LOG.info("Comparing Riak postboxes to Cassandra within the data range {} - {} )", startDate, endDate);
        StreamingOperation<IndexEntry> postboxIds = riakRepo.streamPostBoxIds(startDate, endDate);
        List<Future> tasks = new ArrayList<>(idBatchSize * 3);
        Iterators.partition(postboxIds.iterator(), idBatchSize).forEachRemaining(
                pboxidBatch -> {
                    pboxidBatch.forEach(pboxId -> {
                        Future task = executor.submit(() -> {
                            compareAndLogTheDiff(pboxId.getObjectKey(), true);
                        });
                        tasks.add(task);
                    });

                });
        return tasks;
    }

    void compareAndLogTheDiff(String pboxId, boolean riakToCassandra) {
        if (riakToCassandra) {
            riakPostboxCounter.inc();
        } else {
            cassPostboxCounter.inc();
        }

        PostBox riakPbox = riakRepo.getById(pboxId);
        // We are skipping empty riak postboxes, no point in comparing.
        if (riakToCassandra && (riakPbox.getConversationThreads().isEmpty())) {
            emptyRiakPostboxCounter.inc();
            return;
        }

        PostBox cassPbox = cassRepo.getById(pboxId);

        LOG.debug("Comparing postboxes for id '{}' ", pboxId);
        if (!riakPbox.equals(cassPbox)) {
            // We can potentially remove synchronization here with no functional impact, but that would make logs harder to read
            synchronized (riakPbox) {
                LOG.error("Postbox id '{}' do not match that of {}!", pboxId, riakToCassandra ? "riak" : "cass");
                LOG.info("Riak postbox: {}", riakPbox);
                LOG.info("Cass postbox: {}\n", cassPbox);

                if (LOG.isDebugEnabled()) {
                    getMismatchedMessages(riakPbox, cassPbox);
                }

                MISMATCH_LOG.info(pboxId);
            }

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
                MISMATCH_LOG.debug("NewRepliesCounter riak {}, cass {}", riakpbox.getNewRepliesCounter(), casspbox.getNewRepliesCounter());
            }

            List<AbstractConversationThread> riakCt = new ArrayList<>(riakpbox.getConversationThreads());
            List<AbstractConversationThread> cassCt = new ArrayList<>(casspbox.getConversationThreads());

            Collections.sort(riakCt, PostBox.MODIFICATION_DATE.reversed());
            Collections.sort(cassCt, PostBox.MODIFICATION_DATE.reversed());

            if (riakCt.size() != cassCt.size()) {

                MISMATCH_LOG.debug("Number of conversations riak {}, cass {}", riakCt.size(), cassCt.size());
                int lastIdx = riakCt.size() > cassCt.size() ? riakCt.size() : cassCt.size();

                IntStream.range(0, lastIdx)
                        // This filters out equal conversations
                        .filter(i -> {
                            if (riakCt.size() > i && cassCt.size() > i) {

                                return riakCt.get(i).equals(cassCt.get(i));
                            } else {

                                return true;
                            }
                        })
                        // This prints out the differences
                        .forEach(i -> {

                            if (cassCt.size() > i && riakCt.size() > i) {

                                MISMATCH_LOG.debug("Message mismatching  \nriak: {} \ncass: {} ", riakCt.get(i), cassCt.get(i));
                            } else if (cassCt.size() > i) {

                                MISMATCH_LOG.debug("Message missing from riak \ncass: {} ", cassCt.get(i));
                            } else if (riakCt.size() > i) {

                                MISMATCH_LOG.debug("Message missing from cass \nriak: {} ", riakCt.get(i));
                            } else {

                                throw new RuntimeException("Filtering issue in getMismatchedMessages!");
                            }
                        });
            }
        }
    }

    public List<Future> compareCassToRiakAsync() throws RiakException {
        LOG.info("Comparing Cassandra postboxes to Riak within the data range {} - {}", startDate, endDate);
        Stream<String> postboxIds = cassRepo.streamMessageBoxIdsByHour(startDate.toDate(), endDate.toDate());
        List<Future> tasks = new ArrayList<>();
        Iterators.partition(postboxIds.iterator(), idBatchSize).forEachRemaining(
                pboxidBatch -> {
                    pboxidBatch.forEach(pboxId -> {
                        Future task = executor.submit(() -> {
                            compareAndLogTheDiff(pboxId, false);
                        });
                        tasks.add(task);
                    });
                });

        return tasks;
    }
}
