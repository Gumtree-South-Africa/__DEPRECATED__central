package nl.marktplaats.integration.sendername;

import com.google.common.io.ByteStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.hamcrest.Matchers;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenderNameIntegrationTest extends ReceiverTestsSetup {
    @Test(groups = {"receiverTests"})
    public void rtsProcessedAnAsqMailAndModifiesSenderName() throws Exception {
        deliverMailToRts("plain-asq.eml", "0e7aac86-32d1-4693-978d-0a646458c41a");
        WiserMessage anonymizedAsq = runner.waitForMessageArrival(1, 5000L);
        MimeMessage anonAsq = anonymizedAsq.getMimeMessage();
        assertHasSingleTo(anonAsq, "obeuga@foon.nl", "O. Beuga via Marktplaats");
    }

    private void assertHasSingleTo(MimeMessage anonAsq, String toAddress, String toPersonal) throws Exception {
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO).length, is(1));
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO)[0], Matchers.is(new InternetAddress(toAddress, toPersonal)));
    }

    private void deliverMailToRts(String emlName, String mailId) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        runner.deliver(mailId, emlData, 5);
    }
}
