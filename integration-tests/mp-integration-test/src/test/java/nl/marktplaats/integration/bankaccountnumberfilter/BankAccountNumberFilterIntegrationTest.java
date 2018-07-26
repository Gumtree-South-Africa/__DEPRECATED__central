package nl.marktplaats.integration.bankaccountnumberfilter;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

public class BankAccountNumberFilterIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = {"receiverTests"})
    public void rtsBlocksMailWithFraudulentBankAccount() throws Exception {
        deliverMailToRts("buyer-asq.eml", "31fa6c18-c5a8-47be-b499-eb6008709626");
        runner.waitForMessageArrival(1, 5000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-123456.eml", "bbb1805d-e1f0-443f-b6d1-acf38b022e62");
        runner.assertMessageDoesNotArrive(2, 1000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-654321.eml", "a9664924-2389-487e-8620-6857b79df36a");
        runner.waitForMessageArrival(2, 5000L);
    }

    private void deliverMailToRts(String emlName, String mailId) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        runner.deliver(mailId, emlData, 5);
    }

    private void deliverReplyMailToRts(int replyToMessageNumber, String emlName, String mailIdentifier) throws Exception {
        WiserMessage lastMessage = runner.getRtsSentMail(replyToMessageNumber);
        String anonymousSender = lastMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousSender);
        runner.deliver(mailIdentifier, replyData.getBytes("US-ASCII"), 5);
    }
}
