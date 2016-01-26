package com.ecg.replyts.app.mailreceiver;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.ClusterMode;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class FilesystemMailDataProvider implements MailDataProvider {


    static final String FAILED_PREFIX = "f_";
    static final String FAILED_DIRECTORY_NAME = "failed";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FilesystemMailDataProvider.class);
    static final String INCOMING_FILE_PREFIX = "pre_";
    static final String PROCESSING_FILE_PREFIX = "inwork_";
    private static final IncomingMailFileFilter INCOMING_FILE_FILTER = new IncomingMailFileFilter(INCOMING_FILE_PREFIX);
    private final File mailDataDir;
    private final ClusterModeManager clusterModeManager;
    private final File failedDir;
    private MessageProcessingCoordinator consumer;
    private final FileFilter failedFileFilter;

    private static final Counter FAILED_COUNTER = TimingReports.newCounter("processing_failed");

    public FilesystemMailDataProvider(File mailDataDir, int retryDelay, int retryCounter, MessageProcessingCoordinator consumer, ClusterModeManager clusterModeManager) {
        this.mailDataDir = mailDataDir;
        this.clusterModeManager = clusterModeManager;
        this.failedDir = new File(mailDataDir, "failed");
        this.consumer = consumer;

        if (!this.failedDir.exists() && !this.failedDir.mkdirs()) {
            throw new IllegalStateException(
                    "Couldn't create 'failure' directory, will stop.");
        }

        String failedFileNameFilterPattern = "^(?!(" + FAILED_PREFIX + "){" + (retryCounter + 1) + "})" + FAILED_PREFIX + ".*$";

        this.failedFileFilter = new FailedMailFileFilter(Pattern.compile(
                failedFileNameFilterPattern), retryDelay);

        LOG.info("Scanning Folder {} for files starting with {}", mailDataDir.
                getAbsolutePath(), INCOMING_FILE_PREFIX);
    }

    @Override
    public void run() {

        FileProcessor processor = new FileProcessor();
        if (!processor.performFileProcessing()) {
            sleep();
        }
    }

    private void sleep() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for mails", e);
            Thread.currentThread().interrupt();
        }
    }

    private void process(File tempFilename, File originalFilename) {

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(tempFilename))) {

            consumer.accept(inputStream);

        } catch (Exception e) {
            FAILED_COUNTER.inc();

            File failedFilename = new File(failedDir, FAILED_PREFIX + originalFilename.getName());
            LOG.error("Mail processing failed. Storing mail in failed folder as '" + failedFilename.getName() + "'", e);

            if (!tempFilename.renameTo(failedFilename)) {
                LOG.error("Failed to move failed mail to 'failed' directory " + tempFilename.getName());
            }

        } finally {
            if (tempFilename.exists() && !tempFilename.delete()) {
                LOG.error("Failed to delete mail {} from dropfolder after successful processing", tempFilename.getName());
            }
        }
    }

    @Override
    public void prepareLaunch() {
        LOG.info("Searching Dropfolder for orphaned {}* files and renaming them to {}*", PROCESSING_FILE_PREFIX, INCOMING_FILE_PREFIX);
        new ScanAndRename(mailDataDir, PROCESSING_FILE_PREFIX, INCOMING_FILE_PREFIX).execute();
    }

    class FileProcessor {

        boolean performFileProcessing() {

            if (doNotProcessMessagesNow()) {
                return false;
            }
            File[] files = mailDataDir.listFiles(INCOMING_FILE_FILTER);
            File[] failedFiles = failedDir.listFiles(failedFileFilter);
            File[] allFiles = new File[files.length + failedFiles.length];
            System.arraycopy(files, 0, allFiles, 0, files.length);
            System.arraycopy(failedFiles, 0, allFiles, files.length, failedFiles.length);

            if (allFiles.length == 0) {
                return false;
            }
            for (File f : allFiles) {

                if (doNotProcessMessagesNow()) {
                    return false;
                }

                File tempFilename = new File(mailDataDir, PROCESSING_FILE_PREFIX + f.getName());
                boolean wasRenamed = f.renameTo(tempFilename);
                if (wasRenamed) {
                    process(tempFilename, f);
                }

            }
            return true;
        }

        private boolean doNotProcessMessagesNow() {
            // in blocked mode, there is a datacenter connection loss. Do not continue reading new mails.
            return clusterModeManager.determineMode() == ClusterMode.BLOCKED;
        }
    }
}
