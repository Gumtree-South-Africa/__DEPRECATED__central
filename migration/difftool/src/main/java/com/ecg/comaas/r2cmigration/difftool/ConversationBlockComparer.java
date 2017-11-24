package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ecg.comaas.r2cmigration.difftool.Comparer.waitForCompletion;

public class ConversationBlockComparer {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockComparer.class);

    static void compareRiakToCassandraConv(R2CConversationBlockDiffTool convBlockDiff, String... conversationIds) throws RiakException {
        convBlockDiff.compareRiakToCassandra(conversationIds);
    }

    static void compareRiakToCassandraConv(R2CConversationBlockDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareRiakToCassandra();
        waitForCompletion(tasks);

        if (!diffTool.isRiakMatchesCassandra) {
            LOG.info("ConversationBlock in Riak and Cassandra are different.");
            LOG.info("Riak content does not match that of Cassandra. See the logs for details.");
            LOG.info("Riak to Cassandra comparison - mismatching conversationBlock: {}", R2CConversationBlockDiffTool.RIAK_TO_CASS_CONV_MISMATCH_COUNTER.getCount());
        } else {
            LOG.info("Success! ConversationBlock in Riak and Cassandra are NOT different.");
        }

        long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
        double speed = 0;

        if (diffTool.riakConversationBlockCounter.getCount() != 0 && timepassed != 0) {
            speed = diffTool.riakConversationBlockCounter.getCount() / timepassed;
        }

        LOG.info("------------------------------");
        LOG.info("ConversationBlocks found in Riak: {}", diffTool.riakConversationBlockCounter.getCount());
        LOG.info("ConversationBlocks from Riak found in Cassandra: {}", diffTool.cassConversationBlockCounter.getCount());
        LOG.info("Mismatched conversationBlocks: {}", R2CConversationBlockDiffTool.RIAK_TO_CASS_CONV_MISMATCH_COUNTER.getCount());
        LOG.info("Comparison completed in {} seconds, speed {} conversationBlocks/s", timepassed, speed);
        LOG.info("------------------------------");
    }
}
