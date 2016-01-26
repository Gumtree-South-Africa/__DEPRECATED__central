package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AutomatedMailRemoverTest {

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    private final AutomatedMailRemover mailRemover = new AutomatedMailRemover();

    @Test
    public void testBounceMails() throws Exception {

        File bounceMailFolder = new File(getClass().getResource("/mailremover/bounce-mails").getFile());
        File[] bounceMails = bounceMailFolder.listFiles();
        for (File file : bounceMails) {
            check(file, true);
        }
    }

    @Test
    public void testValidMails() throws Exception {
        File validMailFolder = new File(getClass().getResource("/mailremover/valid-mails").getFile());

        File[] bounceMails = validMailFolder.listFiles();
        for (File file : bounceMails) {
            check(file, false);
        }
    }

    private void check(File f, boolean mustBeBlocked) throws Exception {

        try (FileInputStream fin = new FileInputStream(f)) {
            MessageProcessingContext ctx = new MessageProcessingContext(new Mails().readMail(ByteStreams.toByteArray(fin)), "1", processingTimeGuard);

            mailRemover.preProcess(ctx);

            if (mustBeBlocked) {
                assertTrue(f.getAbsolutePath() + " should be terminated", ctx.isTerminated());
                assertEquals(f.getAbsolutePath() + " should be terminated", MessageState.IGNORED, ctx.getTermination().getEndState());
            } else {
                assertFalse(f.getAbsolutePath() + " should not be terminated", ctx.isTerminated());
            }
        }
    }
}
