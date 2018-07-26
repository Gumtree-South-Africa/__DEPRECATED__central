package nl.marktplaats.integration.anonymizebody;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnonymizeBodyIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = {"receiverTests"})
    public void anonymizeMailInBodyForRecognizedFragments() throws Exception {
        {
            deliverMailToRts("plain-asq.eml", "af8d1519-5387-4ce0-848a-63a013d68a11");
            WiserMessage forwardedAsq = runner.waitForMessageArrival(1, 5000L);

            String plainPart = extractPlainText(forwardedAsq);
            assertThat(plainPart, containsString("U kunt mij mailen op: buyer66@hotmail.com"));
            String htmlPart = extractHtml(forwardedAsq);
            assertThat(htmlPart, containsString("U kunt mij mailen op: buyer66@hotmail.com"));
        }

        {
            deliverReplyMailToRts("non-anonymous-reply.eml", "1a81d8c4-fef0-465f-b916-37a56f80ffdd");
            WiserMessage forwardedReply = runner.waitForMessageArrival(2, 5000L);
            String anonymousReplySender = forwardedReply.getEnvelopeSender();

            String plainPart = extractPlainText(forwardedReply);
            assertThat(plainPart, containsString("Mijn mail address is obeuga@foon.nl."));
            assertThat(plainPart, containsString(String.format("Aan: O. Beuga [%s]", anonymousReplySender)));
            String htmlPart = extractHtml(forwardedReply);
            assertThat(htmlPart, containsString("Mijn mail address is obeuga@foon.nl."));
            assertThat(htmlPart, containsString(String.format("Aan: O. Beuga [%s]", anonymousReplySender)));
        }
    }

    private String extractPlainText(WiserMessage wiserMessage) throws IOException, MessagingException {
        Multipart multipart = (Multipart) wiserMessage.getMimeMessage().getContent();
        String content = (String) multipart.getBodyPart(0).getContent();
        return content.replace("\r\n", "\n");
    }

    private String extractHtml(WiserMessage wiserMessage) throws IOException, MessagingException {
        Multipart multipart = (Multipart) wiserMessage.getMimeMessage().getContent();
        String content = (String) multipart.getBodyPart(1).getContent();
        return content.replace("\r\n", "\n");
    }

    private void deliverMailToRts(String emlName, String mailId) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        runner.deliver(mailId, emlData, 5);
    }

    private void deliverReplyMailToRts(String emlName, String mailIdentifier) throws Exception {
        WiserMessage asqMessage = runner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        runner.deliver(mailIdentifier, replyData.getBytes("US-ASCII"), 5);
    }
}
