package nl.marktplaats.integration.bankaccountnumberfilter;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

public class BankAccountNumberFilterIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = { "receiverTests" })
    public void rtsBlocksMailWithFraudulentBankAccount() throws Exception {
        deliverMailToRts("buyer-asq.eml");
        runner.waitForMessageArrival(1, 5000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-123456.eml");
        runner.assertMessageDoesNotArrive(2, 1000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-654321.eml");
        runner.waitForMessageArrival(2, 5000L);
    }

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        runner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(int replyToMessageNumber, String emlName) throws Exception {
        WiserMessage lastMessage = runner.getRtsSentMail(replyToMessageNumber);
        String anonymousSender = lastMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousSender);
        runner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
