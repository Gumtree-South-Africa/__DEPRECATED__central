package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.fs;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ResponsivenessFilesystemSinkTest {
    private static final int FLUSH_EVERY_N_EVENTS = 1;
    private static final int MAX_BUFFERED_EVENTS = 2;
    private static final String PREFIX_DURING_WRITE = "in-progress-";
    private static final String PREFIX_AFTER_FLUSH = "ready-";

    private ResponsivenessFilesystemSink sink;

    private Clock fixedClock;
    private ResponsivenessRecord record;
    private Path tempDirectory;
    private String uniqueWriterId;
    private long now;

    @Before
    public void setUp() throws Exception {
        fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        tempDirectory = Files.createTempDirectory(null);
        tempDirectory.toFile().deleteOnExit();

        uniqueWriterId = "test-" + UUID.randomUUID().toString();
        record = new ResponsivenessRecord(1, 2, "convoId", "msgId", 60, fixedClock.instant());
        now = fixedClock.instant().toEpochMilli();

        sink = new ResponsivenessFilesystemSink(
                tempDirectory.toString(),
                FLUSH_EVERY_N_EVENTS,
                MAX_BUFFERED_EVENTS,
                PREFIX_DURING_WRITE,
                PREFIX_AFTER_FLUSH,
                fixedClock
        );
    }

    @Test
    public void noBuffer_immediatelyWrittenOut() throws Exception {
        sink.storeResponsivenessRecord(uniqueWriterId, record);
        File[] generatedFiles = findGeneratedFiles();

        assertThat(generatedFiles.length, is(1));

        File outputFile = generatedFiles[0];
        assertTrue(outputFile.exists());

        final List<String> fileContents = Files.readAllLines(outputFile.toPath());
        assertThat(fileContents.size(), is(1));
        assertThat(fileContents.get(0), is("1,2,convoId,msgId,60," + now));

        Map<String, List<ResponsivenessRecord>> buffer = sink.getBuffer();
        assertThat(buffer.get(uniqueWriterId).size(), is(0));
    }

    @Test
    public void buffer_onlyWrittenOutOnceFilled() throws Exception {
        sink = new ResponsivenessFilesystemSink(
                tempDirectory.toString(),
                2,
                MAX_BUFFERED_EVENTS,
                PREFIX_DURING_WRITE,
                PREFIX_AFTER_FLUSH,
                fixedClock
        );

        // First one should just be buffered
        sink.storeResponsivenessRecord(uniqueWriterId, record);
        File[] generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(0));

        // Second one should trigger the writing
        sink.storeResponsivenessRecord(uniqueWriterId, record);
        generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(1));
        File outputFile = generatedFiles[0];
        assertTrue(outputFile.exists());

        final List<String> fileContents = Files.readAllLines(outputFile.toPath());
        assertThat(fileContents.size(), is(2));
        assertThat(fileContents.get(0), is("1,2,convoId,msgId,60," + now));
        assertThat(fileContents.get(1), is("1,2,convoId,msgId,60," + now));

        Map<String, List<ResponsivenessRecord>> buffer = sink.getBuffer();
        assertThat(buffer.get(uniqueWriterId).size(), is(0));
    }

    @Test
    public void directoryNotWritable_noExceptions() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        sink.storeResponsivenessRecord(uniqueWriterId, record);

        File[] generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(0));

        assertThat(sink.getBuffer().get(uniqueWriterId).size(), is(1));

        // Dir becomes writable. Buffer should be flushed.
        assertTrue(tempDirectory.toFile().setWritable(true));
        sink.storeResponsivenessRecord(uniqueWriterId, record);

        generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(1));

        assertThat(sink.getBuffer().get(uniqueWriterId).size(), is(0));
    }

    @Test
    public void errorsPreventFlushing_maxBufferNotExceeded() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        sink.storeResponsivenessRecord(uniqueWriterId, record);
        sink.storeResponsivenessRecord(uniqueWriterId, record);
        sink.storeResponsivenessRecord(uniqueWriterId, record);

        Map<String, List<ResponsivenessRecord>> buffer = sink.getBuffer();
        assertThat(buffer.get(uniqueWriterId).size(), is(MAX_BUFFERED_EVENTS));
    }

    @Test
    public void renameFails_noException() throws Exception {
        String targetFileName = tempDirectory.toFile().getAbsolutePath() + File.separator + PREFIX_AFTER_FLUSH + uniqueWriterId + "-" + now;
        File targetFile = new File(targetFileName);
        assertTrue(targetFile.mkdir());
        assertTrue(targetFile.setReadOnly());
        targetFile.deleteOnExit();

        sink.storeResponsivenessRecord(uniqueWriterId, record);

        assertThat("Buffer should've been flushed to in-progress file", sink.getBuffer().get(uniqueWriterId).size(), is(0));
        assertThat("Should be no files 'ready'", findGeneratedFiles().length, is(0));
        assertThat("In-progress files should be untouched", findInProgressFiles().length, is(1));
    }

    @Test
    public void allBuffersFlushedOnShutdown() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        String anotherWriterId = UUID.randomUUID().toString();

        sink.storeResponsivenessRecord(uniqueWriterId, record);
        sink.storeResponsivenessRecord(uniqueWriterId, record);
        sink.storeResponsivenessRecord(anotherWriterId, record);

        assertTrue(tempDirectory.toFile().setWritable(true));

        sink.flushAll();

        File[] generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(2));
    }

    private File[] findGeneratedFiles() {
        return new File(tempDirectory.toString())
                .listFiles((dir, name) ->
                        name.startsWith(PREFIX_AFTER_FLUSH) && new File(dir.getPath() + File.separator + name).isFile());
    }

    private File[] findInProgressFiles() {
        return new File(tempDirectory.toString())
                .listFiles((dir, name) ->
                        name.startsWith(PREFIX_DURING_WRITE) && new File(dir.getPath() + File.separator + name).isFile());
    }
}
