package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.ecg.comaas.r2cmigration.difftool.repo.CassConfigurationRepo;
import com.ecg.comaas.r2cmigration.difftool.util.CommaSeparatedListOptionHandler;
import com.ecg.comaas.r2cmigration.difftool.util.DateTimeOptionHandler;
import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.NotImplementedException;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class Comparer {

    private final ExecutorService executor;

    private final CassConfigurationRepo configurationRepo;

    private final R2CConversationDiffTool convDiff;

    private final R2CPostboxDiffTool pboxDiff;

    @Component
    static class Options {
        @Option(name = "-rc", usage = "Prior to diffing, count the records to be verified")
        boolean fetchRecordCount = false;

        @Option(name = "-r2c", forbids = "-c2r", usage = "Perform Riak To Cassandra Validation")
        boolean riakToCassandra = false;

        @Option(name = "-c2r", forbids = "-r2c", usage = "Perform Cassandra to Riak Validation")
        boolean cassandraToRiak = false;

        @Option(name = "-v", usage = "Verbose")
        boolean verbose = false;

        @Option(name = "-tz", usage = "Add/remove tz minutes from the time range")
        int timezoneShiftInMinutes = 0;

        @Option(name = "-what", required = true, usage = "What to validate/load [conv, mbox, config]")
        String what;

        @Option(name = "-configFile", forbids = {"-endDate", "-startDate", "-r2c", "-c2r", "-tz", "-rc", "-ids"}, usage = "File to load the Configuration from")
        String configFile;

        @Option(name = "-endDate", handler = DateTimeOptionHandler.class, usage = "End validation at DateTime (defaults to current DateTime)")
        DateTime endDateTime;

        @Option(name = "-startDate", handler = DateTimeOptionHandler.class, usage = "Start validation at DateTime (defaults to current DateTime - TTL period)")
        DateTime startDateTime;

        @Option(name = "-ids", forbids = {"-endDate", "-startDate"}, handler = CommaSeparatedListOptionHandler.class, usage = "List of conversationIds, comma separated. DO NOT use with more than a handful of IDs! You have been warned.")
        String[] conversationIds;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Comparer.class);

    @Autowired
    public Comparer(ExecutorService executor, R2CConversationDiffTool convDiff, R2CPostboxDiffTool pboxDiff, CassConfigurationRepo configurationRepo) {
        this.executor = executor;
        this.convDiff = convDiff;
        this.pboxDiff = pboxDiff;
        this.configurationRepo = configurationRepo;
    }

    private byte[] readFileToByteArray(String configFile) throws IOException {
        Path path = Paths.get(configFile);
        return Files.readAllBytes(path);
    }

    static void waitForCompletion(List<Future> tasks) {
        tasks.parallelStream().filter(Objects::nonNull).forEach(t -> {
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
        LOG.info("Command line arguments: {}", Arrays.asList(args));
        Stopwatch timerTotal = Stopwatch.createStarted();
        // To avoid confusing warn message
        System.setProperty("com.datastax.driver.FORCE_NIO", "true");
        ConfigurableApplicationContext context = null;
        try {
            context = new AnnotationConfigApplicationContext(DiffToolConfiguration.class);
            Options diffToolOpts = context.getBean(Options.class);
            CmdLineParser parser = new CmdLineParser(diffToolOpts);
            try {
                parser.parseArgument(args);
            } catch (CmdLineException e) {
                // handling of wrong arguments
                System.err.println(e.getMessage());
                parser.printUsage(System.err);
                throw (e);
            }
            context.getBean(Comparer.class).execute(diffToolOpts);
            LOG.info("Comparison completed in {}ms", timerTotal.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LOG.error("Diff tool fails with ", e);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    private void execute(Options diffToolOpts) throws RiakException, InterruptedException {
        switch (diffToolOpts.what) {
            case "conv":
                convDiff.setDateRange(diffToolOpts.startDateTime, diffToolOpts.endDateTime, diffToolOpts.timezoneShiftInMinutes);

                if (diffToolOpts.fetchRecordCount) {
                    LOG.info("About to verify {} conversations entries ", convDiff.getConversationsCountInTimeSlice(diffToolOpts.riakToCassandra));
                }

                if (diffToolOpts.riakToCassandra) {
                    if (diffToolOpts.conversationIds != null) {
                        ConversationComparer.compareRiakToCassandraConv(convDiff, diffToolOpts.conversationIds);
                    } else {
                        ConversationComparer.compareRiakToCassandraConv(convDiff);
                    }
                }

                if (diffToolOpts.cassandraToRiak) {
                    if (diffToolOpts.conversationIds != null) {
                        ConversationComparer.compareCassandraToRiakConv(convDiff, diffToolOpts.conversationIds);
                    } else {
                        ConversationComparer.compareCassToRiakConv(convDiff, diffToolOpts.fetchRecordCount);
                    }
                }
                break;
            case "mbox":
                if (diffToolOpts.conversationIds != null) {
                    throw new NotImplementedException("Postbox cannot be compared by conversationId, only by date range");
                }
                pboxDiff.setDateRange(diffToolOpts.startDateTime, diffToolOpts.endDateTime, diffToolOpts.timezoneShiftInMinutes);
                pboxDiff.setVerbose(diffToolOpts.verbose);

                if (diffToolOpts.fetchRecordCount) {
                    LOG.info("About to verify {} postbox entries", pboxDiff.getMessagesCountInTimeSlice(diffToolOpts.riakToCassandra));
                }

                if (diffToolOpts.riakToCassandra) {
                    PostboxComparer.compareRiakToCassandra(pboxDiff);
                }

                if (diffToolOpts.cassandraToRiak) {
                    PostboxComparer.compareCassToRiak(pboxDiff);
                }
                break;
            case "config":
                if (diffToolOpts.configFile != null) {

                    try {

                        configurationRepo.insertIntoConfiguration(this.readFileToByteArray(diffToolOpts.configFile));
                        LOG.info("Config file {} loaded successfully", diffToolOpts.configFile);
                    } catch (IOException io) {
                        LOG.error("Failed to read file {}", diffToolOpts.configFile, io);
                    }
                }
                break;
            default: {
                String msg = String.format("'%s' unsupported or missing -what operation", diffToolOpts.what);
                throw new UnsupportedOperationException(msg);
            }
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }


}
