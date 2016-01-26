package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CleanerTest {

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Test
    public void unnecessaryHeadersAreRemoved() throws Exception {
        Mail mail = new Mails().readMail(ByteStreams.toByteArray(getClass().getResourceAsStream("CleanerTest-mail.eml")));

        assertThat("original mail contains unnecessary header", containsHeader(mail, "X-Ms-Exchange-Organization-Authsource"), is(true));

        MessageProcessingContext context = new MessageProcessingContext(mail, "1", processingTimeGuard);
        Cleaner cleaner = new Cleaner();
        cleaner.postProcess(context);

        MutableMail outgoingMail = context.getOutgoingMail();

        assertThat("outgoing mail does not contain unnecessary header", containsHeader(outgoingMail, "X-Ms-Exchange-Organization-Authsource"), is(false));

    }

    public boolean containsHeader(Mail mail, String headerName) {
        List<String> decodedHeader = mail.getDecodedHeaders().get(headerName);
        return decodedHeader != null && !decodedHeader.isEmpty();
    }

}
