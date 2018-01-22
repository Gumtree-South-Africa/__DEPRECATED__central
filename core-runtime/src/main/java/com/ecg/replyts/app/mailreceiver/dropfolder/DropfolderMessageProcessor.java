package com.ecg.replyts.app.mailreceiver.dropfolder;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.ClusterMode;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

@Component("dropfolderMessageProcessor")
@ConditionalOnProperty(name = "mail.provider.strategy", havingValue = "fs", matchIfMissing = true)
public class DropfolderMessageProcessor implements MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DropfolderMessageProcessor.class);

    private static final Counter ACTIVE_DROPFOLDER_PROCESSORS_COUNTER = TimingReports.newCounter("active_dropfolder_processors");

    static final String FAILED_DIRECTORY_NAME = "failed";
    static final String FAILED_PREFIX = "f_";
    static final String UNPARSABLE_PREFIX = "x_";
    static final String INCOMING_FILE_PREFIX = "pre_";
    static final String PROCESSING_FILE_PREFIX = "inwork_";

    private static final FileFilter INCOMING_FILE_FILTER = file -> file.isFile() && file.getName().startsWith(INCOMING_FILE_PREFIX);

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
    private final ReentrantLock messageFilesQueueRefillLock = new ReentrantLock();

    @PostConstruct
    public void initialize() {
        this.mailDataDirectory = new File(dropfolder);
        this.failedDirectory = new File(mailDataDirectory, FAILED_DIRECTORY_NAME);

        if (!failedDirectory.exists() && !failedDirectory.mkdirs()) {
            throw new IllegalStateException(format("Couldn't create '%s' directory", FAILED_DIRECTORY_NAME));
        }

        Pattern failureFileNamePattern = Pattern.compile("^(?!(" + FAILED_PREFIX + "){" + (retryCounter + 1) + "})" + FAILED_PREFIX + ".*$");

        this.failedFileFilter = file -> {
            Matcher matcher = failureFileNamePattern.matcher(file.getName());
            return matcher.matches() && isFileOldEnough(file);
        };

        this.abandonedFileNameMatchPattern = "^(?:" + FAILED_PREFIX + "){" + (retryCounter + 1) + "}(?!" + FAILED_PREFIX + ").*$";

        // Only perform the orphaned file scan on initialization

        LOG.info("Searching dropfolder for orphaned {}* files and renaming them to {}*", PROCESSING_FILE_PREFIX, INCOMING_FILE_PREFIX);

        final File[] files = mailDataDirectory.listFiles();
        if (files == null) {
            throw new IllegalStateException("Dropfolder could not be listed: " + dropfolder);
        }
        for (File file : files) {
            if (file.getName().startsWith(PROCESSING_FILE_PREFIX)) {
                File targetName = new File(file.getParent(), INCOMING_FILE_PREFIX + file.getName());

                LOG.info("Cleanup dropfolder: renaming {} to {}", file.getName(), targetName.getName());

                checkState(file.renameTo(targetName), "failed to rename %s to %s", file.getName(), targetName);
            }
        }

        LOG.info("Scanning dropfolder {} every {} ms for files starting with {}", mailDataDirectory.getAbsolutePath(),
                watchRetryDelayMs, INCOMING_FILE_PREFIX);
    }

    private boolean isFileOldEnough(File file) {
        int retryCount = StringUtils.countMatches(file.getName(), FAILED_PREFIX);
        return file.lastModified() < (System.currentTimeMillis() - (retryOnFailedMessagePeriod * 60L * 1000L * retryCount));
    }

    @Override
    public void processNext() throws InterruptedException {
        if (shouldProcessMessagesNow()) {
            Optional<File> nextMessage = nextMessage();
            if (nextMessage.isPresent()) {
                try {
                    ACTIVE_DROPFOLDER_PROCESSORS_COUNTER.inc();
                    LOG.trace("About to submit {} for processing", nextMessage.get());
                    process(nextMessage.get());
                } finally {
                    ACTIVE_DROPFOLDER_PROCESSORS_COUNTER.dec();
                }
                return;
            }
        }

        TimeUnit.MILLISECONDS.sleep(watchRetryDelayMs);
    }

    private Optional<File> nextMessage() {
        File nextFile = messageFilesQueue.poll();
        if (nextFile != null) {
            return Optional.of(nextFile);
        } else {
            tryRefillQueue();
            return Optional.ofNullable(messageFilesQueue.poll());
        }
    }

    /**
     * Makes an to refill a queue of files to process. Since an object of the class is shared among multiple processing
     * threads, only one thread at a time is allowed to refill the queue.
     */
    private void tryRefillQueue() {
        if (!messageFilesQueueRefillLock.tryLock()) {
            return;
        }
        try {
            if (messageFilesQueue.isEmpty()) {
                File[] incoming = mailDataDirectory.listFiles(INCOMING_FILE_FILTER);
                checkNotNull(incoming, "listFiles with incoming filter call produced a null result");
                messageFilesQueue.addAll(Arrays.asList(incoming));
                File[] failed = failedDirectory.listFiles(failedFileFilter);
                checkNotNull(failed, "listFiles with failed filter call produced a null result");
                messageFilesQueue.addAll(Arrays.asList(failed));
                int newSize = messageFilesQueue.size();
                if (newSize > 0) {
                    LOG.trace("New queue size: {}", newSize);
                }
            }
        } finally {
            messageFilesQueueRefillLock.unlock();
        }
    }

    private void process(File originalFilename) {
        File tempFilename = new File(mailDataDirectory, PROCESSING_FILE_PREFIX + originalFilename.getName());

        if (!originalFilename.renameTo(tempFilename)) {
            // Some other thread probably picked up the file already
            return;
        }

        MDC.put(MDCConstants.FILENAME, originalFilename.getName());

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(tempFilename))) {
            messageProcessor.accept(inputStream);
        } catch (ParsingException e) {
            UNPARSEABLE_COUNTER.inc();
            File unparsableFilename = new File(failedDirectory, UNPARSABLE_PREFIX + originalFilename.getName());

            if (!tempFilename.renameTo(unparsableFilename)) {
                LOG.error("Failed to move unparsable mail to 'failed' directory {}", tempFilename.getName());
            }
        } catch (Exception e) {
            RETRIED_MESSAGE_COUNTER.inc();

            File failedFilename = new File(failedDirectory, FAILED_PREFIX + originalFilename.getName());

            if (failedFilename.getName().matches(abandonedFileNameMatchPattern)) {
                ABANDONED_RETRY_COUNTER.inc();

                LOG.error("Mail processing abandoned for file: '{}'", failedFilename.getName(), e);
            } else {
                LOG.warn("Mail processing failed. Storing mail in failed folder as '{}'", failedFilename.getName(), e);
            }

            if (!tempFilename.renameTo(failedFilename)) {
                LOG.error("Failed to move failed mail to 'failed' directory {}", tempFilename.getName());
            }
        } finally {
            if (tempFilename.exists() && !tempFilename.delete()) {
                LOG.error("Failed to delete mail {} from dropfolder after successful processing", tempFilename.getName());
            }
        }
    }

    private boolean shouldProcessMessagesNow() {
        return clusterModeManager == null || clusterModeManager.determineMode() != ClusterMode.BLOCKED;
    }
}