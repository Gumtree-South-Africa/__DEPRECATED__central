package nl.marktplaats.integration.knowngoodfilter;

import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.google.common.io.CharStreams;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

/**
 * Created by reweber on 22/10/15
 */
public class KnownGoodFilterIntegrationTest {

    @BeforeMethod(groups = { "receiverTests" })
    public void makeSureRtsIsRunningAndClearRtsSentMails() {
        IntegrationTestRunner.getReplytsRunner();
        IntegrationTestRunner.clearMessages();
        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());
        //TODO replyTsConfigClient.putConfiguration(new Configuration());
    }

    @Test(groups = { "receiverTests" })
    public void rtsBlocksMessagesAfterReachingVolumeThresholdForRegularUser() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th will exceed the threshold.
        for (int i = 0; i < 11; i++) {
            deliverMailToRts("plain-abq.eml", "untrusted-seller@hotmail.com");
        }

        IntegrationTestRunner.waitForMessageArrival(10, 30000L);
        IntegrationTestRunner.assertMessageDoesNotArrive(11, 1000L);
    }

    @Test(groups = { "receiverTests" })
    public void rtsAllowsMessagesAfterReachingVolumeThresholdForKnownGoodUser() throws Exception {
        // Volume filter allows up to 10 mails per 10 minutes.
        // The 11th and beyond will exceed the threshold, but as user is trusted it will arrive
        for (int i = 0; i < 15; i++) {
            deliverMailToRts("plain-abq-trusted.eml", "trusted-seller@hotmail.com");
        }

        IntegrationTestRunner.waitForMessageArrival(15, 40000L);
    }

    private void deliverMailToRts(String emlName, String senderEmailAddress) throws Exception {
        String emlDataStr = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("knowngood/" + emlName), "US-ASCII"));
        emlDataStr = emlDataStr.replace("{{{{initiatorEmailAddress}}}}", senderEmailAddress);
        byte[] emlData = emlDataStr.getBytes("US-ASCII");
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }
}
