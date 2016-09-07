package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.fs;

import ca.kijiji.replyts.user_behaviour.responsiveness.UserResponsivenessListener;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

/**
 * Writes out {@link ResponsivenessRecord}s onto the filesystem.
 * <p>
 * Buffers and then batches the writes. Note that the operations here
 * are not generally thread-safe. However, by design, RTS uses a single thread
 * to process listeners. Therefore, this class should NOT be used outside of
 * processing threads.
 * <p>
 * Records are written out to a temporary file and then renamed.
 */
@Component
public class ResponsivenessFilesystemSink {
    private static final Logger LOG = LoggerFactory.getLogger(ResponsivenessFilesystemSink.class);

    private final String targetDirectoryPath;
    private final int flushEveryNEvents;
    private final int maxBufferedEvents;
    private final String filenamePrefixDuringWrite;
    private final String filenamePrefixAfterFlush;
    private final Clock clock;

    private final Map<String, List<ResponsivenessRecord>> buffer = new HashMap<>();

    private final Counter lostEventsCounter = newCounter("user-behaviour.responsiveness.fs-flush.lostEvents");
    private final Counter failedRenames = newCounter("user-behaviour.responsiveness.fs-flush.failedRenames");
    private final Counter failedWrites = newCounter("user-behaviour.responsiveness.fs-flush.failedWrites");
    private final Timer flushTimer = newTimer("user-behaviour.responsiveness.fs-flush.flushTimer");

    @Autowired
    public ResponsivenessFilesystemSink(
            @Value("${user-behaviour.responsiveness.fs-export.dir}") String targetDirectoryPath,
            @Value("${user-behaviour.responsiveness.fs-export.everyNEvents}") int flushEveryNEvents,
            @Value("${user-behaviour.responsiveness.fs-export.maxBufferedEvents}") int maxBufferedEvents,
            @Value("${user-behaviour.responsiveness.fs-export.fileNamePrefixDuringWrite}") String filenamePrefixDuringWrite,
            @Value("${user-behaviour.responsiveness.fs-export.fileNamePrefixAfterFlush}") String filenamePrefixAfterFlush,
            @Qualifier("responsivenessFSSinkClock") Clock clock
    ) {
        this.targetDirectoryPath = targetDirectoryPath;
        this.flushEveryNEvents = flushEveryNEvents;
        this.maxBufferedEvents = maxBufferedEvents;
        this.filenamePrefixDuringWrite = filenamePrefixDuringWrite;
        this.filenamePrefixAfterFlush = filenamePrefixAfterFlush;
        this.clock = clock;

        File targetDir = new File(targetDirectoryPath);
        if (!targetDir.exists() && !targetDir.mkdir()) {
            throw new IllegalStateException("Target dir '" + targetDirectoryPath + "' does not exist and could not be created.");
        } else if (!targetDir.canWrite() && !targetDir.setWritable(true)) {
            throw new IllegalStateException("Target dir '" + targetDirectoryPath + "' is not writable.");
        }

        LOG.info(
                "Initialized [{}] for writing every {} events. Buffering a max of {}. File prefix: [{}]",
                targetDirectoryPath,
                flushEveryNEvents,
                maxBufferedEvents,
                filenamePrefixAfterFlush
        );
    }

    /**
     * Write out the provided record onto the filesystem.
     * <p>
     * The operation is buffered, so the provided data may not be written out right away.
     *
     * @param uniqueWriterId Unique identifier of the caller, such as thread id.
     *                       This will be part of the filename.
     * @param record         The record to write out.
     */
    public void storeResponsivenessRecord(@Nonnull String uniqueWriterId, @Nonnull ResponsivenessRecord record) {
        List<ResponsivenessRecord> recordsForWriter = buffer.getOrDefault(uniqueWriterId, new ArrayList<>(flushEveryNEvents));

        if (recordsForWriter.size() >= maxBufferedEvents) {
            lostEventsCounter.inc();
            LOG.warn("Buffer for writer [{}] exceeded limit [{}]. Lost event: {}", uniqueWriterId, maxBufferedEvents, record);
            return;
        }

        recordsForWriter.add(record);
        if (!buffer.containsKey(uniqueWriterId)) {
            buffer.put(uniqueWriterId, recordsForWriter);
        }

        if (recordsForWriter.size() < flushEveryNEvents) {
            return;
        }

        flushBuffer(uniqueWriterId, recordsForWriter);
    }

    private void flushBuffer(String writerId, List<ResponsivenessRecord> recordsForWriter) {
        LOG.debug("Flushing buffer for writer [{}]", writerId);

        if (recordsForWriter.isEmpty()) {
            return;
        }

        long now = clock.instant().toEpochMilli();
        String pathPrefix = targetDirectoryPath + File.separator;
        String pathSuffix = writerId + "-" + now;
        File inProgressFile = new File(pathPrefix + filenamePrefixDuringWrite + pathSuffix);
        File finalTargetFile = new File(pathPrefix + filenamePrefixAfterFlush + pathSuffix);

        try (Timer.Context ignored = flushTimer.time()) {
            try (FileWriter fileWriter = new FileWriter(inProgressFile)) {
                BufferedWriter writer = IOUtils.buffer(fileWriter);
                writer.append(recordsToCSV(recordsForWriter)).flush();
            }
            recordsForWriter.clear();
            if (!inProgressFile.renameTo(finalTargetFile)) {
                failedRenames.inc();
                LOG.warn("Couldn't rename [{}] to [{}] for writer [{}]", inProgressFile.getPath(), finalTargetFile.getPath(), writerId);
            }
        } catch (IOException e) {
            failedWrites.inc();
            LOG.warn("Couldn't flush buffered data for writer [{}]", writerId, e);
        }
    }

    // For testing
    Map<String, List<ResponsivenessRecord>> getBuffer() {
        return ImmutableMap.copyOf(buffer);
    }

    private String recordsToCSV(List<ResponsivenessRecord> records) {
        return records.stream().map(this::recordToCSV).collect(Collectors.joining("\n"));
    }

    private String recordToCSV(ResponsivenessRecord record) {
        return record.getVersion() + "," + record.getUserId() + "," + record.getConversationId()
                + "," + record.getMessageId() + "," + record.getTimeToRespondInSeconds()
                + "," + record.getTimestamp().toEpochMilli();
    }

    /**
     * Flush all buffered data.
     * <p>
     * This should only be called once by {@link UserResponsivenessListener} at shut down.
     */
    public void flushAll() {
        LOG.info("Flushing all buffers");
        for (Map.Entry<String, List<ResponsivenessRecord>> bufferForWriter : buffer.entrySet()) {
            flushBuffer(bufferForWriter.getKey(), bufferForWriter.getValue());
        }
    }
}
