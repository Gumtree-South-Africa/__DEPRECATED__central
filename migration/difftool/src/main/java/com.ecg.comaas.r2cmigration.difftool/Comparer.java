package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.google.common.base.Stopwatch;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;


public class Comparer {

    @Component
    static class Options {
        @Option(name = "-rc", usage = "Prior to verification fetch the records to be verified count")
        boolean fetchRecordCount = false;

        @Option(name = "-r2c", usage = "Perform Riak To Cassandra Validation")
        boolean riakToCassandra = true;

        @Option(name = "-c2r", usage = "Perform Cassandra to Riak Validation")
        boolean cassandraToRiak = false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Comparer.class);

    private static void waitForCompletion(List<Future> tasks) {
        tasks.parallelStream().forEach(t -> {
            try {
                t.get();
            } catch (InterruptedException in) {
                LOG.error("Not completed", in);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOG.error("ExecutionException", e);
            }
        });
    }

    static boolean compareRiakToCassandra(RiakCassandraDiffTool diffTool) throws RiakException {
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

    static boolean compareCassToRiak(RiakCassandraDiffTool diffTool) throws RiakException {
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

    public static void main(String[] args) {
        Stopwatch timerTotal = Stopwatch.createStarted();
        // To avoid confusing warn message
        System.setProperty("com.datastax.driver.FORCE_NIO", "true");
        RiakCassandraDiffTool diffTool = null;

        try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(DiffToolConfiguration.class)) {

            Options diffToolOpts = context.getBean(Options.class);
            CmdLineParser parser = new CmdLineParser(diffToolOpts);
            try {
                parser.parseArgument(args);
            } catch (CmdLineException e) {
                // handling of wrong arguments
                System.err.println(e.getMessage());
                parser.printUsage(System.err);
                System.exit(0);
            }

            diffTool = context.getBean(RiakCassandraDiffTool.class);

            LOG.info("Comparing Riak data to Cassandra");
            if (diffToolOpts.fetchRecordCount) {
                LOG.info("About to verify {} conversations in total", diffTool.getConversationsToBeMigratedCount());
            }

            boolean success = true;
            // Start riak to cassandra comparison
            if (diffToolOpts.riakToCassandra) {
                success = compareRiakToCassandra(diffTool);
            }
            if(!success) {
                LOG.info("Number of conversation events that do not match after riak to cass comparison {}", diffTool.RIAK_TO_CASS_EVENT_MISMATCH_COUNTER.getCount());
            }

            // Start cassandra to riak comparison
            if (success && diffToolOpts.cassandraToRiak) {
                LOG.info("Comparing Cassandra data to Riak");
                success = compareCassToRiak(diffTool);
            } else {
                LOG.info("Skipping Cassandra to Riak comparison due to previous differences or selected options");
            }
            if(!success) {
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
            LOG.info("Comparison completed in {}ms", timerTotal.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LOG.error("Diff tool fails with ", e);
        }
    }
}
