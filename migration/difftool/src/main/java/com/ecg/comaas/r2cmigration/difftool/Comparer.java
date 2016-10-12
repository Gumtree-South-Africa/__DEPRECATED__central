package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.ecg.comaas.r2cmigration.difftool.util.DateTimeOptionHandler;
import com.google.common.base.Stopwatch;
import org.joda.time.DateTime;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

import static com.ecg.comaas.r2cmigration.difftool.ConversationComparer.*;
import static com.ecg.comaas.r2cmigration.difftool.PostboxComparer.*;

@Service
public class Comparer {

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private R2CConversationDiffTool convDiff;

    @Autowired
    private R2CPostboxDiffTool pboxDiff;

    @Component
    static class Options {
        @Option(name = "-rc", usage = "Prior to verification fetch the records to be verified count")
        boolean fetchRecordCount = false;

        @Option(name = "-r2c", forbids = "-c2r", usage = "Perform Riak To Cassandra Validation")
        boolean riakToCassandra = false;

        @Option(name = "-c2r", forbids = "-r2c", usage = "Perform Cassandra to Riak Validation")
        boolean cassandraToRiak = false;

        @Option(name = "-what", required = true, usage = "What to validate [conv,mbox]")
        String what;

        @Option(name = "-endDate", handler = DateTimeOptionHandler.class, usage = "End validation at DateTime (defaults to current DateTime)")
        DateTime endDateTime;

        @Option(name = "-startDate", handler = DateTimeOptionHandler.class, usage = "Start validation at DateTime (defaults to current DateTime - TTL period)")
        DateTime startDateTime;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Comparer.class);

    static void waitForCompletion(List<Future> tasks) {
        tasks.parallelStream().forEach(t -> {
            try {
                t.get();
            } catch (InterruptedException in) {
                LOG.error("Not completed", in);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOG.error("ExecutionException", e);
            } catch (Exception e) {
                LOG.error("Execution during comparison", e);
            }
        });
    }

    public static void main(String[] args) {
        Stopwatch timerTotal = Stopwatch.createStarted();
        // To avoid confusing warn message
        System.setProperty("com.datastax.driver.FORCE_NIO", "true");

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
            Comparer comparer = context.getBean(Comparer.class);
            comparer.execute(context, diffToolOpts);
            LOG.info("Comparison completed in {}ms", timerTotal.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LOG.error("Diff tool fails with ", e);
        }
    }

    void execute(ConfigurableApplicationContext context, Options diffToolOpts) throws RiakException, InterruptedException {
        switch (diffToolOpts.what) {
            case "conv":
                convDiff.setDateRange(diffToolOpts.startDateTime, diffToolOpts.endDateTime);

                if (diffToolOpts.fetchRecordCount) {
                    LOG.info("About to verify {} conversations entries ", convDiff.getConversationsToBeMigratedCount());
                }

                if (diffToolOpts.riakToCassandra) {
                    compareRiakToCassandraConv(convDiff);
                }

                if (diffToolOpts.cassandraToRiak) {
                    compareCassToRiakConv(convDiff);
                }
                break;
            case "mbox":
                pboxDiff.setDateRange(diffToolOpts.startDateTime, diffToolOpts.endDateTime);

                if (diffToolOpts.fetchRecordCount) {
                    LOG.info("About to verify {} postbox entries", pboxDiff.getMessagesToBeMigratedCount());
                }

                if (diffToolOpts.riakToCassandra) {
                    compareRiakToCassandra(pboxDiff);
                }

                if (diffToolOpts.cassandraToRiak) {
                    compareCassToRiak(pboxDiff);
                }
                break;
            default:
                throw new UnsupportedOperationException("Please define -what option!");
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
