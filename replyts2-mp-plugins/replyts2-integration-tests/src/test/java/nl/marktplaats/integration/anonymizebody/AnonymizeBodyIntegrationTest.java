package nl.marktplaats.integration.anonymizebody;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.postprocessor.anonymizebody.AnonymiseEmailPostProcessorFactory;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by reweber on 22/10/15
 */
public class AnonymizeBodyIntegrationTest {

    private List<String> patterns = ImmutableList.of(
            "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *(?:<a[^>]*>)?[^<>\\s]*@[^<>\\s]*(?:</a>)?",
            "<span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *> *(?:<a[^>]*>)?[^<>\\s]*@[^<>\\s]*(?:</a>)?",
            "<b><span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *></b> *(?:<a[^>]*>)?[^<>\\s]*@[^<>\\s]*(?:</a>)?",
            "<span[^>]*><b>(From|To|Sender|Receiver|Van|Aan) *: *</b></span *> *(?:<a[^>]*>)?[^<>\\s]*@[^<>\\s]*(?:</a>)?",
            "\\b(From|To|Sender|Receiver|Van|Aan) *: *(<|&lt;)?[^<>\\s]*@[^<>\\s]*(>|&gt;)?",
            "\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){1,6}(<|&lt;)?[^<>\\s]*@[^<>\\s]*(>|&gt;)?"
    );

    @BeforeMethod(groups = { "receiverTests" })
    public void makeSureRtsIsRunningAndClearRtsSentMails() throws IOException {
        IntegrationTestRunner.getReplytsRunner();
        IntegrationTestRunner.clearMessages();

        String patternsAsJson = new ObjectMapper().writeValueAsString(patterns);
        JsonNode jsonNode = new ObjectMapper().readValue("{\"anonymizeMailPatterns\":" + patternsAsJson + "}", JsonNode.class);

        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());
        replyTsConfigClient.putConfiguration(new Configuration(new Configuration.ConfigurationId(AnonymiseEmailPostProcessorFactory.class.getName(), "instance-0"), PluginState.ENABLED, 1, jsonNode));
    }

    @Test(groups = { "receiverTests" })
    public void anonymizeMailInBodyForRecognizedFragments() throws Exception {
        {
            deliverMailToRts("plain-asq.eml");
            WiserMessage forwardedAsq = IntegrationTestRunner.waitForMessageArrival(1, 5000L);

            String plainPart = extractPlainText(forwardedAsq);
            assertThat(plainPart, containsString("U kunt mij mailen op: buyer66@hotmail.com"));
            String htmlPart = extractHtml(forwardedAsq);
            assertThat(htmlPart, containsString("U kunt mij mailen op: buyer66@hotmail.com"));
        }

        {
            deliverReplyMailToRts("non-anonymous-reply.eml");
            WiserMessage forwardedReply = IntegrationTestRunner.waitForMessageArrival(2, 5000L);
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

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("anonymizebody/" + emlName));
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = IntegrationTestRunner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("anonymizebody/" + emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
