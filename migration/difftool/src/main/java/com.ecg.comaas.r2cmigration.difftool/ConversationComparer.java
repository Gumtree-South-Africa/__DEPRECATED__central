package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static com.ecg.comaas.r2cmigration.difftool.Comparer.*;

public class ConversationComparer {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationComparer.class);

    public static void compareConversations(R2CConversationDiffTool diffTool, Options diffToolOpts) throws RiakException, InterruptedException {

        LOG.info("Comparing Riak data to Cassandra");
        if (diffToolOpts.fetchRecordCount) {
            LOG.info("About to verify {} conversations in total", diffTool.getConversationsToBeMigratedCount());
        }

        boolean success = true;
        // Start riak to cassandra comparison
        if (diffToolOpts.riakToCassandra) {
            success = compareRiakToCassandraConv(diffTool);
        }
        if (!success) {
            LOG.info("Number of conversation events that do not match after riak to cass comparison {}", diffTool.RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.getCount());
        }

        // Start cassandra to riak comparison
        if (success && diffToolOpts.cassandraToRiak) {
            LOG.info("Comparing Cassandra data to Riak");
            success = compareCassToRiakConv(diffTool);
        } else {
            LOG.info("Skipping Cassandra to Riak comparison due to previous differences or selected options");
        }
        if (!success) {
            LOG.info("Number of conversation events that do not match after cass to riak comparison {}", diffTool.CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.getCount());
        }

        diffTool.threadPoolExecutor.shutdown();
        diffTool.threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);

        long convIdxCountByDate = diffTool.cassRepo.getConversationModByDayCount();
        long convIdxCount = diffTool.cassRepo.getConversationModCount();

        // This is likely to only work on full import, once cassandra db is used directly core_conversation_modification_desc_idx would contain duplicated conversation_id
        // TODO we might need to distinct the values from these indexes first before comparison
        if (convIdxCount != convIdxCountByDate || convIdxCountByDate != diffTool.riakConversationCounter.getCount()) {
            LOG.info("Verify FAILED");
            LOG.info("Counters do not match! Content of core_conversation_modification_desc_idx (count:{} ) and core_conversation_modification_desc_idx_by_day (count:{})" +
                            "do not match the number of conversation found in Riak (count:{}) ",
                    convIdxCount, convIdxCountByDate, diffTool.riakConversationCounter.getCount());
        }
        if (diffToolOpts.cassandraToRiak && diffToolOpts.riakToCassandra) {
            if (diffTool.riakConversationCounter.getCount() >= diffTool.cassConversationCounter.getCount()) {
                LOG.info("Verify FAILED");
                LOG.info("cassConversationCounter = {}, riakConversationCounter = {}",
                        diffTool.cassConversationCounter.getCount(), diffTool.riakConversationCounter.getCount());
            }
            if (diffToolOpts.cassandraToRiak && diffTool.riakEventCounter.getCount() >= diffTool.cassEventCounter.getCount()) {
                LOG.info("Verify FAILED");
                LOG.info("cassEventCounter = {}, riakEventCounter = {}",
                        diffTool.cassEventCounter.getCount(), diffTool.riakEventCounter.getCount());
            }
        }
    }

    static boolean compareRiakToCassandraConv(R2CConversationDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareRiakToCassandra();
        waitForCompletion(tasks);
        if (diffTool.isRiakMatchesCassandra) {
            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            LOG.info("Compared {} Riak conversations, {} ConversationEvents, completed in {}s, speed {} conversations/s",
                    diffTool.riakConversationCounter.getCount(),
                    diffTool.riakEventCounter.getCount(), timepassed,
                    (diffTool.riakConversationCounter.getCount() != 0 ? (diffTool.riakConversationCounter.getCount() / (timepassed)) : "")
            );
        } else {
            LOG.info("Verify FAILED");
            LOG.info("Riak content does not match that of Cassandra. See the logs for details.");
        }
        return diffTool.isRiakMatchesCassandra;
    }

    static boolean compareCassToRiakConv(R2CConversationDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareCassandraToRiak();
        waitForCompletion(tasks);
        if (diffTool.isCassandraMatchesRiak) {
            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            LOG.info("Compared {} cassandra Conversations, {} ConversationEvents, completed in {}s, speed {} conversations/s",
                    diffTool.cassConversationCounter.getCount(), diffTool.cassEventCounter.getCount(), timepassed,
                    (diffTool.cassConversationCounter.getCount() != 0 ? (diffTool.cassConversationCounter.getCount() / (timepassed)) : "")
            );
        } else {
            LOG.info("Verify FAILED");
            LOG.info("Cassandra content does not match that of Riak. See the logs for details.");
        }
        return diffTool.isCassandraMatchesRiak;
    }
}
