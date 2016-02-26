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

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CleanerTest {

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Test
    public void unnecessaryHeadersAreRemovedOthersRetained() throws Exception {
        Mail mail = new Mails().readMail(ByteStreams.toByteArray(getClass().getResourceAsStream("CleanerTest-mail.eml")));

        List<String> headersToRemove = Arrays.asList(
                "X-Ms-Exchange-Organization-Authsource",
                "X-Originalarrivaltime"
        );

        List<String> headersToKeep = Arrays.asList(
                "Subject",
                "Date",
                "Content-Type",
                "Content-ID",
                "Content-Disposition",
                "Content-Transfer-Encoding",
                "MIME-Version",
                "Precedence",
                "X-Precedence",
                "X-Auto-Response-Suppress",
                "Auto-Submitted"
        );

        // Verify incoming mail
        headersToRemove.forEach(headerName -> assertContainsHeader("original mail", mail, headerName));
        headersToKeep.forEach(headerName -> assertContainsHeader("original mail", mail, headerName));

        MessageProcessingContext context = new MessageProcessingContext(mail, "1", processingTimeGuard);
        Cleaner cleaner = new Cleaner();
        cleaner.postProcess(context);

        MutableMail outgoingMail = context.getOutgoingMail();

        // Verify outgoing mail
        headersToRemove.forEach(headerName -> assertDoesNotContainsHeader("outgoing mail", outgoingMail, headerName));
        headersToKeep.forEach(headerName -> assertContainsHeader("outgoing mail", outgoingMail, headerName));
    }

    private void assertContainsHeader(String mailDescription, Mail mail, String headerName) {
        assertHeader(mailDescription, mail, headerName, true);
    }

    private void assertDoesNotContainsHeader(String mailDescription, Mail mail, String headerName) {
        assertHeader(mailDescription, mail, headerName, false);
    }

    private void assertHeader(String mailDescription, Mail mail, String headerName, boolean headerExpected) {
        assertThat(mailDescription + " contains header " + headerName, containsHeader(mail, headerName), is(headerExpected));
    }

    private boolean containsHeader(Mail mail, String headerName) {
        List<String> decodedHeader = mail.getDecodedHeaders().get(headerName);
        return decodedHeader != null && !decodedHeader.isEmpty();
    }

}
