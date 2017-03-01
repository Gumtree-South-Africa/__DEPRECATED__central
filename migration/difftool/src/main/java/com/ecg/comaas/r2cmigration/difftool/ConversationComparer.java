package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ecg.comaas.r2cmigration.difftool.Comparer.waitForCompletion;

public class ConversationComparer {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationComparer.class);

    static void compareRiakToCassandraConv(R2CConversationDiffTool convDiff, String... conversationIds) throws RiakException {
        convDiff.compareRiakToCassandra(conversationIds);
    }

    static void compareRiakToCassandraConv(R2CConversationDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareRiakToCassandra();
        waitForCompletion(tasks);

        if (!diffTool.isRiakMatchesCassandra) {
            LOG.info("DATA in Riak and Cassandra IS DIFFERENT");
            LOG.info("Riak content does not match that of Cassandra. See the logs for details.");
            LOG.info("Riak to Cassandra comparison - mismatching conversations: {}, conversation events: {}",
                    R2CConversationDiffTool.RIAK_TO_CASS_CONV_MISMATCH_COUNTER.getCount(), R2CConversationDiffTool.RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.getCount());
        }

        long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
        double speed = 0;

        if (diffTool.riakConversationCounter.getCount() != 0 && timepassed != 0) {
            speed = diffTool.riakConversationCounter.getCount() / timepassed;
        }
        LOG.info("Compared {} Riak conversations, {} ConversationEvents, completed in {} seconds, speed {} conversations/s",
                diffTool.riakConversationCounter.getCount(),
                diffTool.riakEventCounter.getCount(), timepassed, speed);
    }

    static void compareCassandraToRiakConv(R2CConversationDiffTool convDiff, String... conversationIds) throws RiakException {
        convDiff.compareCassandraToRiak(conversationIds);
    }

    static void compareCassToRiakConv(R2CConversationDiffTool diffTool, boolean fetchConvCount) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareCassandraToRiak();
        waitForCompletion(tasks);
        if (!diffTool.isCassandraMatchesRiak) {
            LOG.info("DATA in Cassandra and Riak IS DIFFERENT");
            LOG.info("Cassandra content does not match that of Riak. See the logs for details.");
            LOG.info("Cassandra to Riak comparison - mismatching conversations: {}, conversation events: {}",
                    R2CConversationDiffTool.CASS_TO_RIAK_CONV_MISMATCH_COUNTER.getCount(), R2CConversationDiffTool.CASS_TO_RIAK_EVENT_MISMATCH_COUNTER.getCount());
        }

        long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
        double speed = 0;
        if (diffTool.cassConversationCounter.getCount() != 0 && timepassed != 0) {
            speed = diffTool.cassConversationCounter.getCount() / timepassed;
        }
        LOG.info("Compared {} cassandra Conversations, {} ConversationEvents, completed in {} seconds, speed {} conversations/s",
                diffTool.cassConversationCounter.getCount(), diffTool.cassEventCounter.getCount(), timepassed, speed);

        // This times out when count reaches about 100K
        if (fetchConvCount) {
            long convIdxCountByDate = diffTool.getCassandraConversationModByDayCount();
            long convIdxCount = diffTool.getCassandraConversationCount();

            // This is likely to only work on full import, once cassandra db is used directly core_conversation_modification_desc_idx would contain duplicated conversation_id
            // so we might need to distinct the values from these indexes first before comparison
            if (convIdxCount != convIdxCountByDate) {
                LOG.info("DATA in Cassandra and Riak IS DIFFERENT");
                LOG.info("Counters do not match! Content of core_conversation_modification_desc_idx (count:{} ) " +
                        "and core_conversation_modification_desc_idx_by_day (count:{})", convIdxCount, convIdxCountByDate);
            }
        }
    }
}
