package nl.marktplaats.integration.bankaccountnumberfilter;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import nl.marktplaats.filter.bankaccount.BankAccountFilterFactory;
import nl.marktplaats.integration.support.ReceiverTestsSetup;
import org.subethamail.wiser.WiserMessage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BankAccountNumberFilterIntegrationTest extends ReceiverTestsSetup {

    private static final String BAN_FILTER_PROPERTIES = "replyts/mpplugin_BankAccountNumberFilter.properties";
    private static final String BAN_FILTER_NAME = "com.ebay.ecg.csba.replyts.filter.bankaccount.BankAccountFilter";

    private List<String> fraudulentBankAccounts;

    private ReplyTsConfigClient replyTsConfigClient;
    private Configuration.ConfigurationId configurationId = new Configuration.ConfigurationId(BankAccountFilterFactory.class.getName(), "instance-1");

    @BeforeMethod(groups = { "receiverTests" })
    public void makeSureRtsIsRunningAndClearRtsSentMails() throws IOException {
        fraudulentBankAccounts = new ArrayList<>();
        fraudulentBankAccounts.add("123456");
        fraudulentBankAccounts.add("987654321");
        fraudulentBankAccounts.add("87238935");

        String asJson = new ObjectMapper().writeValueAsString(fraudulentBankAccounts);
        JsonNode jsonNode = new ObjectMapper().readValue("{\"fraudulentBankAccounts\":" + asJson + "}", JsonNode.class);

        replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());
        replyTsConfigClient.putConfiguration(new Configuration(configurationId, PluginState.ENABLED, 1, jsonNode));
    }

    // DISABLED because Bankaccount filter is still WIP
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsBlocksMailWithFraudulentBankAccount() throws Exception {
        deliverMailToRts("buyer-asq.eml");
        IntegrationTestRunner.waitForMessageArrival(1, 5000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-123456.eml");
        IntegrationTestRunner.assertMessageDoesNotArrive(2, 1000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-654321.eml");
        IntegrationTestRunner.waitForMessageArrival(2, 5000L);
    }

    // DISABLED because Bankaccount filter is still WIP
    @Test(groups = { "receiverTests" }, enabled = false)
    public void rtsReportsFraudsterWhileBlockingMailFromVictim() throws Exception {
        deliverMailToRts("buyer-asq.eml");
        IntegrationTestRunner.waitForMessageArrival(1, 5000L);

        deliverReplyMailToRts(1, "seller-reply-bank-account-654321.eml");
        IntegrationTestRunner.waitForMessageArrival(2, 5000L);

        addFraudulentBankAccountNumber("654321");

        deliverReplyMailToRts(2, "buyer-reply-bank-account-654321.eml");
        IntegrationTestRunner.assertMessageDoesNotArrive(3, 5000L);

        assertProcessingFeedbackWithFraudster("654321", "obeuga@foon.nl");
    }

    private void addFraudulentBankAccountNumber(String bankAccountNumber) throws IOException {
        fraudulentBankAccounts.add(bankAccountNumber);

        String asJson = new ObjectMapper().writeValueAsString(fraudulentBankAccounts);
        JsonNode jsonNode = new ObjectMapper().readValue("{\"fraudulentBankAccounts\":" + asJson + "}", JsonNode.class);

        replyTsConfigClient.deleteConfiguration(configurationId);
        replyTsConfigClient.putConfiguration(new Configuration(configurationId, PluginState.ENABLED, 1, jsonNode));

        // TODO: proper wait for reload
        try {
            Thread.sleep(3L * 1000L);
        } catch (InterruptedException ignore) {}
    }

    private void assertProcessingFeedbackWithFraudster(String expectedBankAccountNumber, String expectedFraudsterMail) {
        // TODO how to get this feedback here?
        //Map<String,Object> latestProcessingFeedback =
        //        new ProcessingFeedbackRepository().getLatestProcessingFeedback(BAN_FILTER_NAME);
        //assertThat((String) latestProcessingFeedback.get("description"), startsWith(expectedFraudsterMail + "|"));
        //assertThat((String) latestProcessingFeedback.get("uiHint"), startsWith(expectedBankAccountNumber));
    }

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("bankaccountnumberfilter/" + emlName));
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(int replyToMessageNumber, String emlName) throws Exception {
        WiserMessage lastMessage = IntegrationTestRunner.getRtsSentMail(replyToMessageNumber);
        String anonymousSender = lastMessage.getEnvelopeSender();

        String replyData = CharStreams.toString(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("bankaccountnumberfilter/" + emlName)));
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
