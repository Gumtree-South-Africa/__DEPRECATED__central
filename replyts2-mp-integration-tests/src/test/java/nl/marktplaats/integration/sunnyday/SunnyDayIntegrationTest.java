package nl.marktplaats.integration.sunnyday;

import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * Created by reweber on 22/10/15
 */
public class SunnyDayIntegrationTest {

    @BeforeMethod(groups = { "receiverTests" })
    public void makeSureRtsIsRunningAndClearRtsSentMails() {
        IntegrationTestRunner.getReplytsRunner();
        IntegrationTestRunner.clearMessages();
    }

    @Test(groups = { "receiverTests" })
    public void rtsProcessedAnAsqMailAndAReply() throws Exception {
        deliverMailToRts("plain-asq.eml");
        WiserMessage anonymizedAsq = IntegrationTestRunner.waitForMessageArrival(1, 5000L);
        MimeMessage anonAsq = anonymizedAsq.getMimeMessage();
        assertThat(anonAsq.getSubject(), is("Reactie op uw advertentie: Twee matrassen, hoef je niet te draaien en wasbare hoezen"));
        assertThat(anonymizedAsq.getEnvelopeReceiver(), is("obeuga@foon.nl"));
        // asserts that sendername plugin works:
        assertHasSingleTo(anonAsq, "obeuga@foon.nl", "O. Beuga via Marktplaats");
        assertThat(anonymizedAsq.getEnvelopeSender(), isAnonymized());
        assertHasAnonymousFrom(anonAsq);
        assertRtsHeadersNotPresent(anonAsq);
        assertIsAnonymous(anonymizedAsq, "seller_66@hotmail.com");

        deliverReplyMailToRts("plain-asq-reply.eml");
        WiserMessage anonymizedAsqReply = IntegrationTestRunner.waitForMessageArrival(2, 5000L);
        MimeMessage anonAsqReply = anonymizedAsqReply.getMimeMessage();
        assertThat(anonAsqReply.getSubject(), is("Antw: Reactie op uw advertentie: Twee matrassen, hoef je niet te draaien en wasbare hoezen"));
        assertThat(anonymizedAsqReply.getEnvelopeReceiver(), is("seller_66@hotmail.com"));
        // asserts that sendername plugin works for replies:
        assertHasSingleTo(anonAsqReply, "seller_66@hotmail.com", "Seller66 via Marktplaats");
        assertThat(anonymizedAsqReply.getEnvelopeSender(), isAnonymized());
        assertHasAnonymousFrom(anonAsqReply);
        assertRtsHeadersNotPresent(anonAsqReply);
        assertIsAnonymous(anonymizedAsqReply, "obeuga@foon.nl");
    }

    private void assertHasSingleTo(MimeMessage anonAsq, String toAddress, String toPersonal) throws Exception {
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO).length, is(1));
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO)[0], Matchers.<Address>is(new InternetAddress(toAddress, toPersonal)));
    }

    private void assertHasAnonymousFrom(MimeMessage anonAsq) throws MessagingException {
        assertThat(anonAsq.getFrom().length, is(1));
        assertThat(anonAsq.getFrom()[0], isAnonymizedAddress());
    }

    private Matcher<? super String> isAnonymized() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String address) {
                return address.endsWith("@test-platform.com");
            }

            public void describeTo(Description description) {
                description.appendText("anonymous email address");
            }
        };
    }

    private Matcher<? super Address> isAnonymizedAddress() {
        return new TypeSafeMatcher<Address>() {
            @Override
            protected boolean matchesSafely(Address address) {
                return address instanceof InternetAddress &&
                        ((InternetAddress) address).getAddress().endsWith("@test-platform.com");
            }

            public void describeTo(Description description) {
                description.appendText("anonymous email address");
            }
        };
    }

    private void assertRtsHeadersNotPresent(MimeMessage anonMail) throws MessagingException {
        assertThat(anonMail.getHeader("X-ADID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-L1-CATEGORYID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-L2-CATEGORYID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-FROM"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-TO"), is(nullValue()));
    }

    private void assertIsAnonymous(WiserMessage anonMail, String absentMailAddress) throws Exception {
        // NOTE: only works on not so fancy encodings, no base64 please
        assertThat(new String(anonMail.getData(), "US-ASCII"), not(containsString(absentMailAddress)));
    }

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("sunnyday/" + emlName));
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = IntegrationTestRunner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("sunnyday/" + emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
