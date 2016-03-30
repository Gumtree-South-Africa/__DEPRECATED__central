package nl.marktplaats.integration.volumefilter;

import com.google.common.io.CharStreams;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.joda.time.DateTime;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.util.Date;


public class VolumeFilterIntegrationTest extends ReceiverTestsSetup {

    @Test(groups = { "receiverTests" })
    public void rtsBlocksMessagesAfterReachingVolumeThreshold() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        // Note: we need a different ad id for each mail, otherwise all mails will join the same conversation.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-asq.eml", "volume-seller-11@hotmail.com", "m" + i);
        }

        runner.waitForMessageArrival(10, 30000L);
        runner.assertMessageDoesNotArrive(11, 1000L);
    }

    @Test(groups = { "receiverTests" })
    public void rtsDoesNotBlockMessagesAfterTimeoutHasPassed() throws Exception {
        //
        // First step: verify we can fake events
        //
        String controlSenderEmailAddress = "persistent-volume-seller-123@hotmail.com";
        for (int i = 0; i < 11; i++) {
            getSession().execute("INSERT INTO volume_events(user_id, received_time) VALUES(?, NOW())", controlSenderEmailAddress);
        }
        deliverMailToRts("plain-asq.eml", controlSenderEmailAddress, "m1");

        // If the following assertion does not hold, something went wrong with the above events, probably because
        // the volume filter plugin uses a different schema.
        runner.assertMessageDoesNotArrive(1, 2000L);
        runner.clearMessages();

        //
        // Second step: verify timeout is effective
        //
        String senderEmailAddress = "persistent-volume-seller-456@hotmail.com";
        for (int i = 0; i < 11; i++) {
            Date date = new DateTime().minusMinutes(11).minusSeconds(i).toDate();
            getSession().execute("INSERT INTO volume_events(user_id, received_time) VALUES(?, maxTimeuuid(?))", senderEmailAddress, date);
        }

        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-asq.eml", senderEmailAddress, "m" + i);
        }

        runner.waitForMessageArrival(10, 30000L);
        runner.assertMessageDoesNotArrive(11, 1000L);
    }

    @Test(groups = { "receiverTests" })
    public void rtsDoesNotBlocksReplies() throws Exception {
        deliverMailToRts("plain-asq.eml", "volume-seller-88@gmail.com", "m1");
        runner.waitForMessageArrival(1, 5000L);

        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th and 12th should not exceed the threshold.
        for (int i = 0; i < 12; i++) {
            deliverReplyMailToRts("plain-asq-reply.eml");
        }

        runner.waitForMessageArrival(13, 30000L);
    }

    private void deliverMailToRts(String emlName, String senderEmailAddress, String adId) throws Exception {
        String emlDataStr = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName), "US-ASCII"));
        emlDataStr = emlDataStr.replace("{{{{senderEmailAddress}}}}", senderEmailAddress);
        emlDataStr = emlDataStr.replace("{{{{adId}}}}", adId);
        byte[] emlData = emlDataStr.getBytes("US-ASCII");
        runner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = runner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream(emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        runner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
