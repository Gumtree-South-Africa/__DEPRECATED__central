package com.ecg.replyts.app.mailreceiver;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.ClusterMode;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

@Component("mailDataProvider")
@ConditionalOnProperty(name = "mail.provider.strategy", havingValue = "fs", matchIfMissing = true)
public class DropfolderMessageProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DropfolderMessageProcessor.class);

    private static final Counter FAILED_COUNTER = TimingReports.newCounter("processing_failed");
    private static final Counter ABANDONED_RETRY_COUNTER = TimingReports.newCounter("processing_failed_abandoned");

    static final String FAILED_DIRECTORY_NAME = "failed";
    static final String FAILED_PREFIX = "f_";
    static final String INCOMING_FILE_PREFIX = "pre_";
    static final String PROCESSING_FILE_PREFIX = "inwork_";

    static final FileFilter INCOMING_FILE_FILTER = file -> file.isFile() && file.getName().startsWith(INCOMING_FILE_PREFIX);

    @Autowired(required = false)
    private ClusterModeManager clusterModeManager;

    @Value("${mailreceiver.filesystem.dropfolder}")
    private String dropfolder;

    @Value("${mailreceiver.retrydelay.minutes}")
    private int retryOnFailedMessagePeriod;

    @Value("${mailreceiver.watch.retrydelay.millis:1000}")
    private int watchRetryDelayMs;

    @Value("${mailreceiver.retries}")
    private int retryCounter;

    @Autowired
    private MessageProcessingCoordinator messageProcessor;

    private File mailDataDirectory;

    private File failedDirectory;

    private FileFilter failedFileFilter;

    private String abandonedFileNameMatchPattern;

    private final BlockingQueue<File> messageFilesQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void initialize() {
        this.mailDataDirectory = new File(dropfolder);
        this.failedDirectory = new File(mailDataDirectory, FAILED_DIRECTORY_NAME);

        checkArgument(this.mailDataDirectory.isDirectory(), "%s is not a directory", mailDataDirectory);

        if (!failedDirectory.exists() && !failedDirectory.mkdirs()) {
            throw new IllegalStateException(format("Couldn't create '%s' directory", FAILED_DIRECTORY_NAME));
        }

        Pattern failureFileNamePattern = Pattern.compile("^(?!(" + FAILED_PREFIX + "){" + (retryCounter + 1) + "})" + FAILED_PREFIX + ".*$");

        this.failedFileFilter = file -> {
            Matcher matcher = failureFileNamePattern.matcher(file.getName());
            boolean fileIsOldEnough = file.lastModified() < (System.currentTimeMillis() - (retryOnFailedMessagePeriod * 60L * 1000L));

            return matcher.matches() && fileIsOldEnough;
        };

        this.abandonedFileNameMatchPattern = "^(?:" + FAILED_PREFIX + "){" + (retryCounter + 1) + "}(?!" + FAILED_PREFIX + ").*$";

        // Only perform the orphaned file scan on initialization

        LOG.info("Searching dropfolder for orphaned {}* files and renaming them to {}*", PROCESSING_FILE_PREFIX, INCOMING_FILE_PREFIX);

        for (File file : mailDataDirectory.listFiles()) {
            if (file.getName().startsWith(PROCESSING_FILE_PREFIX)) {
                File targetName = new File(file.getParent(), INCOMING_FILE_PREFIX + file.getName());

                LOG.info("Cleanup dropfolder: renaming {} to {}", file.getName(), targetName.getName());

                checkState(file.renameTo(targetName), "failed to rename %s to %s", file.getName(), targetName);
            }
        }

        LOG.info("Scanning dropfolder {} every {} ms for files starting with {}", mailDataDirectory.getAbsolutePath(),
                watchRetryDelayMs, INCOMING_FILE_PREFIX);
    }

    @Override
    public void processNext() throws InterruptedException {
        if (shouldProcessMessagesNow()) {
            Optional<File> nextMessage = nextMessage();
            if (nextMessage.isPresent()) {
                process(nextMessage.get());
                return;
            }
        }

        TimeUnit.MILLISECONDS.sleep(watchRetryDelayMs);
    }

    private Optional<File> nextMessage() {
        File nextFile = messageFilesQueue.poll();
        if (nextFile != null) {
            return Optional.of(nextFile);
        }

        synchronized (messageFilesQueue) {
            if (messageFilesQueue.isEmpty()) {
                File[] incoming = mailDataDirectory.listFiles(INCOMING_FILE_FILTER);
                checkNotNull(incoming, "listFiles with incoming filter call produced a null result");
                messageFilesQueue.addAll(Arrays.asList(incoming));
                File[] failed = failedDirectory.listFiles(failedFileFilter);
                checkNotNull(failed, "listFiles with failed filter call produced a null result");
                messageFilesQueue.addAll(Arrays.asList(failed));
            }
        }
        
        return Optional.ofNullable(messageFilesQueue.poll());
    }

    private void process(File originalFilename) {
        File tempFilename = new File(mailDataDirectory, PROCESSING_FILE_PREFIX + originalFilename.getName());

        if (!originalFilename.renameTo(tempFilename)) {
            // Some other thread probably picked up the file already
            return;
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(tempFilename))) {
            messageProcessor.accept(inputStream);
        } catch (Exception e) {
            FAILED_COUNTER.inc();

            File failedFilename = new File(failedDirectory, FAILED_PREFIX + originalFilename.getName());

            if (failedFilename.getName().matches(abandonedFileNameMatchPattern)) {
                ABANDONED_RETRY_COUNTER.inc();

                LOG.error("Mail processing abandoned for file: '" + failedFilename.getName() + "'", e);
            } else {
                LOG.warn("Mail processing failed. Storing mail in failed folder as '" + failedFilename.getName() + "'", e);
            }

            if (!tempFilename.renameTo(failedFilename)) {
                LOG.error("Failed to move failed mail to 'failed' directory " + tempFilename.getName());
            }
        } finally {
            if (tempFilename.exists() && !tempFilename.delete()) {
                LOG.error("Failed to delete mail {} from dropfolder after successful processing", tempFilename.getName());
            }
            MDC.clear();
        }
    }

    private boolean shouldProcessMessagesNow() {
        return clusterModeManager == null || clusterModeManager.determineMode() != ClusterMode.BLOCKED;
    }
}