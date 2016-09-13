package com.ecg.comaas.r2cmigration.difftool;

import com.ecg.comaas.r2cmigration.difftool.util.DateTimeOptionHandler;
import com.google.common.base.Stopwatch;
import org.joda.time.DateTime;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static com.ecg.comaas.r2cmigration.difftool.ConversationComparer.*;
import static com.ecg.comaas.r2cmigration.difftool.PostboxComparer.*;

public class Comparer {

    @Component
    static class Options {
        @Option(name = "-rc", usage = "Prior to verification fetch the records to be verified count")
        boolean fetchRecordCount = false;

        @Option(name = "-r2c", usage = "Perform Riak To Cassandra Validation")
        boolean riakToCassandra = true;

        @Option(name = "-c2r", usage = "Perform Cassandra to Riak Validation")
        boolean cassandraToRiak = false;

        @Option(name = "-what", required = true, usage = "What to validate [conv,mbox]")
        String what;

        @Option(name = "-endDate", handler = DateTimeOptionHandler.class, usage = "End validation at DateTime (defaults to current DateTime)")
        DateTime endDateTime;
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

            switch(diffToolOpts.what) {
                case "conv":
                    R2CConversationDiffTool convDiffTool = context.getBean(R2CConversationDiffTool.class);
                    convDiffTool.setEndDate(diffToolOpts.endDateTime);
                    compareConversations(convDiffTool, diffToolOpts);
                    break;
                case "mbox":
                    R2CPostboxDiffTool pboxDiffTool = context.getBean(R2CPostboxDiffTool.class);
                    comparePostboxes(pboxDiffTool, diffToolOpts);
                    break;
                default:
                    throw new UnsupportedOperationException("Please define -what option!");
            }

            LOG.info("Comparison completed in {}ms", timerTotal.elapsed(TimeUnit.MILLISECONDS));

        } catch (Exception e) {
            LOG.error("Diff tool fails with ", e);
        }
    }


}
