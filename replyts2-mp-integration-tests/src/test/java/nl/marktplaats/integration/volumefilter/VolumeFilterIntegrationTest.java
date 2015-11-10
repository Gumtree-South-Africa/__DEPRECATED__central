package nl.marktplaats.integration.volumefilter;

import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

public class VolumeFilterIntegrationTest extends ReceiverTestsSetup {

    // DISABLED because filter is still WIP
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsBlocksMessagesAfterReachingVolumeThreshold() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-asq.eml", "volume-seller-11@hotmail.com");
        }

        IntegrationTestRunner.waitForMessageArrival(10, 30000L);
        IntegrationTestRunner.assertMessageDoesNotArrive(11, 1000L);
    }

    // DISABLED because filter is still WIP
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsDoesNotBlockMessagesAfterTimeoutHasPassed() throws Exception {
        String senderEmailAddress = "persistent-volume-seller-123@hotmail.com";

        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-asq.eml", senderEmailAddress);
        }

        IntegrationTestRunner.waitForMessageArrival(10, 30000L);
        IntegrationTestRunner.assertMessageDoesNotArrive(11, 1000L);
        IntegrationTestRunner.clearMessages();

        // Instead of waiting for 10 minutes, the volume recordings are given a different date.
        // TODO how to set value correctly here (probably just override config?)
        //VolumeRepository volumeRepository = new VolumeRepository();
        //volumeRepository.redateVolumeRecordings(
        //        senderEmailAddress, -TimeUnit.MINUTES.toMillis(15));

        // Volume filter AGAIN allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-asq.eml", senderEmailAddress);
        }

        IntegrationTestRunner.waitForMessageArrival(10, 30000L);
        IntegrationTestRunner.assertMessageDoesNotArrive(11, 1000L);
    }

    // DISABLED because filter is still WIP
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsDoesNotBlocksReplies() throws Exception {
        deliverMailToRts("plain-asq.eml", "volume-seller-88@gmail.com");
        IntegrationTestRunner.waitForMessageArrival(1, 1000L);

        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th and 12th should not exceed the threshold.
        for (int i = 0; i < 12; i++) {
            // The volume filter's timestamp has an accuracy of 1 milli second
            Thread.sleep(20);
            deliverReplyMailToRts("plain-asq-reply.eml");
        }

        IntegrationTestRunner.waitForMessageArrival(13, 30000L);
    }

    private void deliverMailToRts(String emlName, String senderEmailAddress) throws Exception {
        String emlDataStr = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("volumefilter/" + emlName), "US-ASCII"));
        emlDataStr = emlDataStr.replace("{{{{senderEmailAddress}}}}", senderEmailAddress);
        byte[] emlData = emlDataStr.getBytes("US-ASCII");
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = IntegrationTestRunner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("volumefilter/" + emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
