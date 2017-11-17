package com.ecg.replyts.app.mailreceiver;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DropfolderMessageProcessorTest.TestContext.class)
@TestPropertySource(properties = {
        "mailreceiver.filesystem.dropfolder = #{systemProperties['java.io.tmpdir']}",
        "mailreceiver.retrydelay.minutes = 5",
        "mailreceiver.watch.retrydelay.millis = 1000",
        "mailreceiver.retries = 5"
})
public class DropfolderMessageProcessorTest {
    @Autowired
    private MessageProcessingCoordinator consumer;

    @Autowired
    private DropfolderMessageProcessor instance;

    private File watchedDirectory;

    @Before
    public void setup() {
        watchedDirectory = spy(new File(System.getProperty("java.io.tmpdir")));
        ReflectionTestUtils.setField(instance, "mailDataDirectory", watchedDirectory);
    }

    @After
    public void tearDown() throws Exception {
        for (File file : new File(System.getProperty("java.io.tmpdir")).listFiles(new TempFileFilter())) {
            file.delete();
        }
        for (File file : new File(System.getProperty("java.io.tmpdir"), DropfolderMessageProcessor.FAILED_DIRECTORY_NAME).listFiles(new TempFileFilter())) {
            file.delete();
        }
    }

    @Test
    public void workerWillPickNextFileOnRenameFailure() throws Exception {
        File file1 = mock(File.class);
        when(file1.renameTo(any(File.class))).thenReturn(Boolean.FALSE);
        File temp = File.createTempFile("junit-temp", "tempfile");
        File file2 = spy(temp);
        when(watchedDirectory.listFiles(any(FileFilter.class))).thenReturn(new File[]{file1, file2}, new File[]{file2});

        instance.processNext();
        instance.processNext();

        verify(file1, times(1)).renameTo(any(File.class));
        verify(file2, times(1)).renameTo(any(File.class));
        verify(consumer, times(1)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillMoveFileOnFailure() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = file.getParentFile().getPath();

        when(watchedDirectory.listFiles(any(FileFilter.class))).thenReturn(new File[]{file});
        doThrow(new IOException()).when(consumer).accept(any(InputStream.class));

        instance.processNext();

        File renamedTempFile = new File(tempDir + "/failed/" + DropfolderMessageProcessor.FAILED_PREFIX + file.getName());

        assertThat(renamedTempFile.exists(), is(true));
        renamedTempFile.deleteOnExit();
    }

    @Test
    public void workerWillPickupNewAndFailedMails() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = file.getParentFile().getPath();
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, DropfolderMessageProcessor.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        instance.processNext();
        instance.processNext();

        verify(consumer, times(2)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillPickupFailedMailsWithoutNewMails() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, DropfolderMessageProcessor.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        instance.processNext();

        verify(consumer, times(1)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillNotPickupTooOldFailedMails() throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, DropfolderMessageProcessor.FAILED_PREFIX + DropfolderMessageProcessor.FAILED_PREFIX + DropfolderMessageProcessor.FAILED_PREFIX + DropfolderMessageProcessor.FAILED_PREFIX + DropfolderMessageProcessor.FAILED_PREFIX + DropfolderMessageProcessor.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        assertTrue(failed.getName().matches("^(?:f_){6}(?!f_).*$"));

        instance.processNext();

        verify(consumer, never()).accept(any(InputStream.class));
    }

    @Test
    public void workerWillNotPickupTooYoungFailedMails() throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, DropfolderMessageProcessor.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 4);

        instance.processNext();

        verify(consumer, never()).accept(any(InputStream.class));
    }

    @Test
    public void workerWillIncreaseFailurePrefixAppropriately() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();

        doThrow(new IOException()).when(consumer).accept(any(InputStream.class));

        for (int i = 1; i <= 6; i++) {
            StringBuilder failedFileName = new StringBuilder();
            for (int j = 1; j <= i; j++) {
                failedFileName.append(DropfolderMessageProcessor.FAILED_PREFIX);
            }
            failedFileName.append(file.getName());

            instance.processNext();

            File currentFile = new File(failuresDir, failedFileName.toString());

            assertThat(currentFile + " doesn't exist", currentFile.exists(), is(true));

            //prepare for next loop
            currentFile.setLastModified(System.currentTimeMillis() - 6 * 1000 * 60);
        }

        //one more try, should not result in invocation of consumer.accept
        instance.processNext();

        //5: last iteration must not result in actual processing
        verify(consumer, times(6)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillDeleteInputFileOnProcessingSuccess() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();

        instance.processNext();

        File[] inprogressFiles = new File(tempDir).listFiles(
            file1 -> file1.getName().startsWith(DropfolderMessageProcessor.PROCESSING_FILE_PREFIX));

        assertThat(inprogressFiles.length, is(0));
    }

    @Test
    public void processNext_whenMailUnparsable_shouldMoveToFailed() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = file.getParentFile().getPath();

        when(watchedDirectory.listFiles(any(FileFilter.class))).thenReturn(new File[]{file});
        doThrow(new ParsingException()).when(consumer).accept(any(InputStream.class));

        instance.processNext();

        File renamedTempFile = new File(tempDir + "/failed/" + DropfolderMessageProcessor.UNPARSABLE_PREFIX + file.getName());

        assertThat(renamedTempFile.exists(), is(true));
        renamedTempFile.deleteOnExit();
    }

    private static class TempFileFilter implements FileFilter {
        File tempDir;
        File failedDir;

        public TempFileFilter() {
            tempDir = new File(System.getProperty("java.io.tmpdir"));
            failedDir = new File(tempDir, DropfolderMessageProcessor.FAILED_DIRECTORY_NAME);
        }

        @Override
        public boolean accept(File file) {
            return (file.getParentFile().equals(tempDir) || file.getParentFile().equals(failedDir)) &&
                    (file.getName().startsWith(DropfolderMessageProcessor.FAILED_PREFIX) ||
                            file.getName().startsWith(DropfolderMessageProcessor.INCOMING_FILE_PREFIX) ||
                            file.getName().startsWith(DropfolderMessageProcessor.PROCESSING_FILE_PREFIX));
        }
    }

    @Configuration
    @Import(DropfolderMessageProcessor.class)
    static class TestContext {
        @MockBean
        private MessageProcessingCoordinator consumer;

        @MockBean
        private ClusterModeManager clusterModeManager;
    }
}
