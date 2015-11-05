package nl.marktplaats.integration.bankaccountnumberfilter;

import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

public class BankAccountNumberFilterIntegrationTest extends ReceiverTestsSetup {

    // DISABLED BECAUSE OF MISSING FILTER RESULT INSPECTOR (AUR-383)
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsBlocksMailWithFraudulentBankAccount() throws Exception {
        deliverMailToRts("buyer-asq.eml");
        IntegrationTestRunner.waitForMessageArrival(1, 500000000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-123456.eml");
        IntegrationTestRunner.assertMessageDoesNotArrive(2, 100000000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-654321.eml");
        IntegrationTestRunner.waitForMessageArrival(2, 50000000L);
    }

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(int replyToMessageNumber, String emlName) throws Exception {
        WiserMessage lastMessage = IntegrationTestRunner.getRtsSentMail(replyToMessageNumber);
        String anonymousSender = lastMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
