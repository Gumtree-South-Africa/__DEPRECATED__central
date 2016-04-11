/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.app.mailreceiver;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.runtime.cluster.ClusterMode;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author alindhorst
 */
@RunWith(MockitoJUnitRunner.class)
public class FilesystemMailDataProviderTest {

    private FilesystemMailDataProvider instance;
    private File watchedDirectory;
    @Mock
    private MessageProcessingCoordinator consumer;

    @Mock
    private ClusterModeManager clusterModeManager;

    @Before
    public void setup() {
        when(clusterModeManager.determineMode()).thenReturn(ClusterMode.OK);
        watchedDirectory = spy(new File(System.getProperty("java.io.tmpdir")));
        instance = new FilesystemMailDataProvider(watchedDirectory, 5, 5, 1000, consumer, clusterModeManager);
    }

    @After
    public void tearDown() throws Exception {
        for (File file : new File(System.getProperty("java.io.tmpdir")).listFiles(new TempFileFilter())) {
            file.delete();
        }
        for (File file : new File(System.getProperty("java.io.tmpdir"), FilesystemMailDataProvider.FAILED_DIRECTORY_NAME).listFiles(new TempFileFilter())) {
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

        boolean success = instance.new FileProcessor().performFileProcessing();
        assertThat(success, is(true));

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

        instance.new FileProcessor().performFileProcessing();

        File renamedTempFile = new File(tempDir + "/failed/" + FilesystemMailDataProvider.FAILED_PREFIX + file.getName());

        assertThat(renamedTempFile.exists(), is(true));
        renamedTempFile.deleteOnExit();
    }

    @Test
    public void workerWillPickupNewAndFailedMails() throws Exception {

        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = file.getParentFile().getPath();
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, FilesystemMailDataProvider.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        instance.new FileProcessor().performFileProcessing();

        verify(consumer, times(2)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillPickupFailedMailsWithoutNewMails() throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, FilesystemMailDataProvider.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        instance.new FileProcessor().performFileProcessing();

        verify(consumer, times(1)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillNotPickupTooOldFailedMails() throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, FilesystemMailDataProvider.FAILED_PREFIX + FilesystemMailDataProvider.FAILED_PREFIX + FilesystemMailDataProvider.FAILED_PREFIX + FilesystemMailDataProvider.FAILED_PREFIX + FilesystemMailDataProvider.FAILED_PREFIX + FilesystemMailDataProvider.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 5);

        instance.new FileProcessor().performFileProcessing();

        verify(consumer, never()).accept(any(InputStream.class));
    }

    @Test
    public void workerWillNotPickupTooYoungFailedMails() throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();
        File failed = new File(failuresDir, FilesystemMailDataProvider.FAILED_PREFIX + "file");
        failed.createNewFile();
        failed.setLastModified(System.currentTimeMillis() - 60 * 1000 * 4);

        instance.new FileProcessor().performFileProcessing();

        verify(consumer, never()).accept(any(InputStream.class));
    }

    @Test
    public void workerWillIncreaseFailurePrefixAppropriately() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();

        FilesystemMailDataProvider.FileProcessor processor = instance.new FileProcessor();
        doThrow(new IOException()).when(consumer).accept(any(InputStream.class));

        for (int i = 1; i <= 6; i++) {
            StringBuilder failedFileName = new StringBuilder();
            for (int j = 1; j <= i; j++) {
                failedFileName.append(FilesystemMailDataProvider.FAILED_PREFIX);
            }
            failedFileName.append(file.getName());

            processor.performFileProcessing();

            File currentFile = new File(failuresDir, failedFileName.toString());

            assertThat(currentFile.exists(), is(true));

            //prepare for next loop
            currentFile.setLastModified(System.currentTimeMillis() - 6 * 1000 * 60);
        }

        //one more try, should not result in invocation of consumer.accept
        processor.performFileProcessing();


        //5: last iteration must not result in actual processing
        verify(consumer, times(6)).accept(any(InputStream.class));
    }

    @Test
    public void workerWillDeleteInputFileOnProcessingSuccess() throws Exception {
        File file = File.createTempFile("pre_junit-temp", "tempfile");
        String tempDir = System.getProperty("java.io.tmpdir");
        File failuresDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        failuresDir.mkdir();

        FilesystemMailDataProvider.FileProcessor processor = instance.new FileProcessor();
        processor.performFileProcessing();

        File[] inprogressFiles = new File(tempDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith(FilesystemMailDataProvider.PROCESSING_FILE_PREFIX);
            }

        });

        assertThat(inprogressFiles.length, is(0));
    }

    private static class TempFileFilter implements FileFilter {

        File tempDir;
        File failedDir;

        public TempFileFilter() {
            tempDir = new File(System.getProperty("java.io.tmpdir"));
            failedDir = new File(tempDir, FilesystemMailDataProvider.FAILED_DIRECTORY_NAME);
        }

        @Override
        public boolean accept(File file) {

            return (file.getParentFile().equals(tempDir) || file.getParentFile().equals(failedDir))
                    && (file.getName().startsWith(FilesystemMailDataProvider.FAILED_PREFIX)
                    || file.getName().startsWith(FilesystemMailDataProvider.INCOMING_FILE_PREFIX)
                    || file.getName().startsWith(FilesystemMailDataProvider.PROCESSING_FILE_PREFIX));
        }
    }
}
