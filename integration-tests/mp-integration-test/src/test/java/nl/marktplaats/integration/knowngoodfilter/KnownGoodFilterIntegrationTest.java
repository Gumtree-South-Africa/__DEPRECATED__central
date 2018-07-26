package nl.marktplaats.integration.knowngoodfilter;

import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

public class KnownGoodFilterIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = {"receiverTests"}, enabled = false)
    public void rtsBlocksMessagesAfterReachingVolumeThresholdForRegularUser() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-abq.eml", "untrusted-seller@hotmail.com", "895118ac-c5e5-4781-9561-4bc03d89a440");
        }

        runner.waitForMessageArrival(10, 30000L);
        runner.assertMessageDoesNotArrive(11, 1000L);
    }

    @Test(groups = {"receiverTests"})
    public void rtsAllowsMessagesAfterReachingVolumeThresholdForKnownGoodUser() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th and beyond will exceed the threshold, but as user is trusted it will arrive
        for (int i = 0; i < 15; i++) {
            deliverMailToRts("plain-abq-trusted.eml", "trusted-seller@hotmail.com", "6f8174be-3855-4bec-a88e-1b2ce4634dfd");
        }

        runner.waitForMessageArrival(15, 40000L);
    }

    private void deliverMailToRts(String emlName, String senderEmailAddress, String mailId) throws Exception {
        String emlDataStr = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName), "US-ASCII"));
        emlDataStr = emlDataStr.replace("{{{{initiatorEmailAddress}}}}", senderEmailAddress);
        byte[] emlData = emlDataStr.getBytes("US-ASCII");
        runner.deliver(mailId, emlData, 5);
    }
}
