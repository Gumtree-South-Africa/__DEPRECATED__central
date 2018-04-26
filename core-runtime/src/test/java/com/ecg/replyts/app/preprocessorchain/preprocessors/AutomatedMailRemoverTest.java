package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class AutomatedMailRemoverTest {

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    private final AutomatedMailRemover mailRemover = new AutomatedMailRemover();

    @Test
    public void testBounceMails() throws Exception {
        Files.list(Paths.get("src/test/resources/mailremover/bounce-mails/")).forEach(path -> check(path.toFile(), true));
    }

    @Test
    public void testValidMails() throws Exception {
        Files.list(Paths.get("src/test/resources/mailremover/valid-mails/")).forEach(path -> check(path.toFile(), false));
    }

    private void check(File f, boolean mustBeBlocked) {

        try (FileInputStream fin = new FileInputStream(f)) {
            MessageProcessingContext ctx = new MessageProcessingContext(Mails.readMail(ByteStreams.toByteArray(fin)), "1", processingTimeGuard);

            mailRemover.preProcess(ctx);

            if (mustBeBlocked) {
                assertTrue(f.getAbsolutePath() + " should be terminated", ctx.isTerminated());
                assertEquals(f.getAbsolutePath() + " should be terminated", MessageState.IGNORED, ctx.getTermination().getEndState());
            } else {
                assertFalse(f.getAbsolutePath() + " should not be terminated", ctx.isTerminated());
            }
        } catch (IOException | ParsingException e) {
            throw Throwables.propagate(e);
        }
    }
}
