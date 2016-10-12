package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ecg.comaas.r2cmigration.difftool.Comparer.waitForCompletion;

public class PostboxComparer {

    private static final Logger LOG = LoggerFactory.getLogger(PostboxComparer.class);

    static void compareRiakToCassandra(R2CPostboxDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareRiakToCassAsync();
        waitForCompletion(tasks);
        LOG.info("Compared {} postbox ids from riak, number of empty postboxes are {} ",
                diffTool.riakPostboxCounter.getCount(), diffTool.emptyRiakPostboxCounter.getCount());

        if (diffTool.isRiakMatchesCassandra) {

            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            double speed = 0;
            if (diffTool.riakPostboxCounter.getCount() != 0 && timepassed != 0) {
                speed = diffTool.riakPostboxCounter.getCount() / timepassed;
            }
            LOG.info("Compared {} Riak postboxes, completed in {}s, speed {} postboxes/s",
                    diffTool.riakPostboxCounter.getCount(), timepassed, speed);
        } else {
            LOG.info("DATA in Riak and Cassandra IS DIFFERENT");
            LOG.info("Riak postboxes do not match that of Cassandra. See the logs for details.");
            LOG.info("Number of postboxes that do not match after Riak to Cassandra comparison {}", diffTool.RIAK_TO_CASS_POSTBOX_MISMATCH_COUNTER.getCount());
        }
    }

    static void compareCassToRiak(R2CPostboxDiffTool diffTool) throws RiakException {
        Stopwatch timerStage = Stopwatch.createStarted();
        List<Future> tasks = diffTool.compareCassToRiakAsync();
        waitForCompletion(tasks);

        if (diffTool.isCassandraMatchesRiak) {

            long timepassed = timerStage.elapsed(TimeUnit.SECONDS);
            double speed = 0;

            if (diffTool.cassPostboxCounter.getCount() != 0 && timepassed != 0) {
                speed = diffTool.cassPostboxCounter.getCount() / timepassed;
            }
            LOG.info("Compared {} cassandra postboxes in {}s, speed {} postboxes/s",
                    diffTool.cassPostboxCounter.getCount(), timepassed, speed);
        } else {
            LOG.info("DATA in Cassandra and Riak IS DIFFERENT");
            LOG.info("Cassandra postboxes do not match that of Riak. See the logs for details.");
            LOG.info("Number of postboxes that do not match after Cassandra to Riak comparison {}", diffTool.CASS_TO_RIAK_POSTBOX_MISMATCH_COUNTER.getCount());
        }
    }
}
