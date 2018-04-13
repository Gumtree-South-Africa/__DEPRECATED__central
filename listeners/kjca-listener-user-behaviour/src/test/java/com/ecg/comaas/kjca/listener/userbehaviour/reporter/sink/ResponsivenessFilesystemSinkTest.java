package com.ecg.comaas.kjca.listener.userbehaviour.reporter.sink;

import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
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

    private ResponsivenessRecord record;
    private Path tempDirectory;
    private String uniqueWriterId;
    private long now;

    @Before
    public void setUp() throws Exception {
        Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
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
                PREFIX_AFTER_FLUSH
        );
    }

    @Test
    public void noBuffer_immediatelyWrittenOut() throws Exception {
        sink.storeRecord(uniqueWriterId, record);
        File[] generatedFiles = findGeneratedFiles();

        assertThat(generatedFiles.length, is(1));

        File outputFile = generatedFiles[0];
        assertTrue(outputFile.exists());

        final List<String> fileContents = Files.readAllLines(outputFile.toPath());
        assertThat(fileContents.size(), is(1));
        assertThat(fileContents.get(0), is("1,2,convoId,msgId,60," + now));
    }

    @Test
    public void buffer_onlyWrittenOutOnceFilled() throws Exception {
        sink = new ResponsivenessFilesystemSink(
                tempDirectory.toString(),
                2,
                MAX_BUFFERED_EVENTS,
                PREFIX_DURING_WRITE,
                PREFIX_AFTER_FLUSH
        );

        // First one should just be buffered
        sink.storeRecord(uniqueWriterId, record);
        File[] generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(0));

        // Second one should trigger the writing
        sink.storeRecord(uniqueWriterId, record);
        generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(1));
        File outputFile = generatedFiles[0];
        assertTrue(outputFile.exists());

        final List<String> fileContents = Files.readAllLines(outputFile.toPath());
        assertThat(fileContents.size(), is(2));
        assertThat(fileContents.get(0), is("1,2,convoId,msgId,60," + now));
        assertThat(fileContents.get(1), is("1,2,convoId,msgId,60," + now));
    }

    @Test
    public void directoryNotWritable_noExceptions() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        // getBuffer used to contain: return ImmutableMap.copyOf(buffer);

        sink.storeRecord(uniqueWriterId, record);

        File[] generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(0));

        // Dir becomes writable. Buffer should be flushed.
        assertTrue(tempDirectory.toFile().setWritable(true));
        sink.storeRecord(uniqueWriterId, record);

        generatedFiles = findGeneratedFiles();
        assertThat(generatedFiles.length, is(1));
    }

    @Test
    public void errorsPreventFlushing_maxBufferNotExceeded() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        sink.storeRecord(uniqueWriterId, record);
        sink.storeRecord(uniqueWriterId, record);
        sink.storeRecord(uniqueWriterId, record);
    }

    @Test
    @Ignore("error prone - attempts to create a dir with the same name having different clock instance")
    public void renameFails_noException() throws Exception {
        String targetFileName = tempDirectory.toFile().getAbsolutePath() + File.separator + PREFIX_AFTER_FLUSH + uniqueWriterId + "-" + now;
        File targetFile = new File(targetFileName);
        assertTrue(targetFile.mkdir());
        assertTrue(targetFile.setReadOnly());
        targetFile.deleteOnExit();

        sink.storeRecord(uniqueWriterId, record);

        assertThat("Should be no files 'ready'", findGeneratedFiles().length, is(0));
        assertThat("In-progress files should be untouched", findInProgressFiles().length, is(1));
    }

    @Test
    @Ignore("error prone - access denied on FS")
    public void allBuffersFlushedOnShutdown() throws Exception {
        assertTrue(tempDirectory.toFile().setWritable(false));

        String anotherWriterId = UUID.randomUUID().toString();

        sink.storeRecord(uniqueWriterId, record);
        sink.storeRecord(uniqueWriterId, record);
        sink.storeRecord(anotherWriterId, record);

        assertTrue(tempDirectory.toFile().setWritable(true));

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
