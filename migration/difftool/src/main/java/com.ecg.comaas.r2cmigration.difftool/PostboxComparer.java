package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ecg.comaas.r2cmigration.difftool.Comparer.Options;
import static com.ecg.comaas.r2cmigration.difftool.Comparer.waitForCompletion;

public class PostboxComparer {

    private static final Logger LOG = LoggerFactory.getLogger(PostboxComparer.class);

    public static void comparePostboxes(R2CPostboxDiffTool diffTool, Options diffToolOpts) throws RiakException, InterruptedException {

        LOG.info("Comparing Riak postboxes to Cassandra");
        if (diffToolOpts.fetchRecordCount) {
            LOG.info("About to verify {} postboxes", diffTool.getMessagesToBeMigratedCount());
        }

        boolean success = true;
        // Start riak to cassandra comparison
        if (diffToolOpts.riakToCassandra) {
            success = compareRiakToCassandra(diffTool);
        }
        if (!success) {
            LOG.info("Number of postboxes that do not match after riak to cass comparison {}", diffTool.RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER.getCount());
        }

        // Start cassandra to riak comparison
        if (success && diffToolOpts.cassandraToRiak) {
            LOG.info("Comparing Cassandra postboxes to Riak");
            success = compareCassToRiak(diffTool);
        } else {
            LOG.info("Skipping Cassandra to Riak comparison due to previous differences or selected options");
        }
        if (!success) {
            LOG.info("Number of postboxes that do not match after cass to riak comparison {}", diffTool.CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER.getCount());
        }

        diffTool.threadPoolExecutor.shutdown();
        diffTool.threadPoolExecutor.awaitTermination(1, TimeUnit.MINUTES);

        if (diffToolOpts.cassandraToRiak && diffToolOpts.riakToCassandra) {
            if (diffTool.riakPostboxCounter.getCount() >= diffTool.cassPostboxCounter.getCount()) {
                LOG.info("Verify FAILED");
                LOG.info("cassPostboxCounter = {}, riakPostboxCounter = {}",
                        diffTool.cassPostboxCounter.getCount(), diffTool.riakPostboxCounter.getCount());
            }
        }
    }

    static boolean compareRiakToCassandra(R2CPostboxDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareRiakToCassAsync();
        waitForCompletion(tasks);
        if (diffTool.isRiakMatchesCassandra) {
            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            LOG.info("Compared {} Riak postboxes, completed in {}s, speed {} postboxes/s",
                    diffTool.riakPostboxCounter.getCount(), timepassed,
                    (diffTool.riakPostboxCounter.getCount() != 0 ? (diffTool.riakPostboxCounter.getCount() / (timepassed)) : "")
            );
        } else {
            LOG.info("Verify FAILED");
            LOG.info("Riak postboxes do not match that of Cassandra. See the logs for details.");
        }
        return diffTool.isRiakMatchesCassandra;
    }

    static boolean compareCassToRiak(R2CPostboxDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareCassToRiakAsync();
        waitForCompletion(tasks);
        if (diffTool.isCassandraMatchesRiak) {
            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            LOG.info("Compared {} cassandra postboxes in {}s, speed {} postboxes/s",
                    diffTool.cassPostboxCounter.getCount(), timepassed,
                    (diffTool.cassPostboxCounter.getCount() != 0 ? (diffTool.cassPostboxCounter.getCount() / (timepassed)) : "")
            );
        } else {
            LOG.info("Verify FAILED");
            LOG.info("Cassandra postboxes do not match that of Riak. See the logs for details.");
        }
        return diffTool.isCassandraMatchesRiak;
    }
}
