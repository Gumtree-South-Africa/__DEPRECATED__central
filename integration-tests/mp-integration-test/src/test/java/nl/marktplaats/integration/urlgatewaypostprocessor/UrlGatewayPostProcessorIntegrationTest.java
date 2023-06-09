package nl.marktplaats.integration.urlgatewaypostprocessor;

import com.google.common.io.ByteStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UrlGatewayPostProcessorIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = {"receiverTests"})
    public void rtsRewritesUrlsInMessageBodyForAsq() throws Exception {
        deliverMailToRts("linking-asq.eml", "2f998c82-c0cb-45dc-a671-985fd9917d25");
        WiserMessage message = runner.waitForMessageArrival(1, 5000L);

        Multipart mainPart = (Multipart) message.getMimeMessage().getContent();
        MimeBodyPart textPart = (MimeBodyPart) mainPart.getBodyPart(0);
        String text = (String) textPart.getContent();
        String plainLinksText = extractLinkPart(text, "plain links");
        assertThat("plainLinksText", plainLinksText, is(
                "http://www.marktplaats.nl\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.com\n" +
                        "http://www.marktplaats.nl@google.com\n" +
                        "http://10.10.10.10\n" +
                        "http://www.marktplaats.nl@10.10.10.10\n" +
                        "tester@test.com\n" +
                        "tester@test.net\n" +
                        "tester@test.nl\n"));
        // NOTE: Username/password links are not replaced as they should be blocked by a filter.

        MimeBodyPart htmlPart = (MimeBodyPart) mainPart.getBodyPart(1);
        String html = (String) htmlPart.getContent();
        String htmlLinks1 = extractLinkPart(html, "html links 1");
        assertThat("htmlLinks1", htmlLinks1, is(
                "<a href=\"http://www.marktplaats.nl\">http://www.marktplaats.nl</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">http://www.google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">www.google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.com\">http://google.com</a><BR />\n" +
                        "<a href=\"http://www.marktplaats.nl\">http://www.marktplaats.nl</a>@google.com<BR />\n" +
                        "http://10.10.10.10<BR />\n" +
                        "<a href=\"http://www.marktplaats.nl\">http://www.marktplaats.nl</a>@10.10.10.10<BR />\n" +
                        "tester@test.com<BR />\n" +
                        "tester@test.net<BR />\n" +
                        "tester@test.nl<BR />\n"));
        // NOTE (again): Username/password links are not treated as they should, however these should be blocked by a filter.
        // Regardless the situation - security wise - is improved, not worsened.

        String htmlLinks2 = extractLinkPart(html, "html links 2");
        assertThat("htmlLinks2", htmlLinks2, is(
                "<a href=\"http://www.marktplaats.nl\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.com\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.marktplaats.nl%40google.com\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2F10.10.10.10\">klik mij!</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.marktplaats.nl%4010.10.10.10\">klik mij!</a><BR />\n" +
                        "<a href=\"mailto:tester@test.com\">klik mij!</a><BR />\n" +
                        "<a href=\"mailto:tester@test.net\">klik mij!</a><BR />\n" +
                        "<a href=\"mailto:tester@test.nl\">klik mij!</a><BR />\n"));

        String htmlLinks3 = extractLinkPart(html, "html links 3");
        assertThat("htmlLinks3", htmlLinks3, is(
                "<a href=\"http://www.marktplaats.nl\">http://www.marktplaats.nl</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">http://www.google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.com\">www.google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.com\">http://google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.marktplaats.nl%40google.com\">http://www.marktplaats.nl@google.com</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2F10.10.10.10\">http://10.10.10.10</a><BR />\n" +
                        "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.marktplaats.nl%4010.10.10.10\">http://www.marktplaats.nl@10.10.10.10</a><BR />\n" +
                        "<a href=\"mailto:tester@test.com\">tester@test.com</a><BR />\n" +
                        "<a href=\"mailto:tester@test.net\">tester@test.net</a><BR />\n" +
                        "<a href=\"mailto:tester@test.nl\">tester@test.nl</a><BR />\n"));
    }

    private String extractLinkPart(String partText, String header) {
        int start = partText.indexOf("--- " + header + " ---");
        start = partText.indexOf("\n", start) + 1;
        int end = partText.indexOf("=== " + header + " ===");
        return partText.substring(start, end).replace("\r\n", "\n");
    }

    private void deliverMailToRts(String emlName, String mailId) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        runner.deliver(mailId, emlData, 5);
    }
}
