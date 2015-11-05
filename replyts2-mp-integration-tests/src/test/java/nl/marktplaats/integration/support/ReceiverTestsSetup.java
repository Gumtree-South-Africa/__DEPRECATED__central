package nl.marktplaats.integration.support;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.marktplaats.filter.bankaccount.BankAccountFilterFactory;
import nl.marktplaats.filter.knowngood.KnownGoodFilterFactory;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReceiverTestsSetup {

    @BeforeGroups(groups = { "receiverTests" })
    public void startEmbeddedRts() throws IOException {
        // TODO: fix confDir hack on next line, it should be picked up by IntegrationTestRunner.setConfigResourceDirectory
        System.setProperty("confDir", "/Users/evanoosten/dev/mp/replyts2/mp-replyts2/replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf");
        IntegrationTestRunner.setConfigResourceDirectory("/mp-integration-test-conf");
        IntegrationTestRunner.getReplytsRunner();

        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());

        // Configure result inspector
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(ResultInspectorFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        new ObjectMapper().createObjectNode()));

        // Configure known good filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(KnownGoodFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        new ObjectMapper().createObjectNode()));

        // Configure bank account filter
        List<String> fraudulentBankAccounts = new ArrayList<>(); // TODO: use Arrays.asList
        fraudulentBankAccounts.add("123456");
        fraudulentBankAccounts.add("987654321");
        fraudulentBankAccounts.add("87238935");

        String asJson = new ObjectMapper().writeValueAsString(fraudulentBankAccounts);
        JsonNode jsonNode = new ObjectMapper().readValue("{\"fraudulentBankAccounts\":" + asJson + "}", JsonNode.class);

        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(BankAccountFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        jsonNode));

//        String patternsAsJson = new ObjectMapper().writeValueAsString(patterns);
//        JsonNode jsonNode = new ObjectMapper().readValue("{\"anonymizeMailPatterns\":" + patternsAsJson + "}", JsonNode.class);
//
//        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());
//        replyTsConfigClient.putConfiguration(new Configuration(new Configuration.ConfigurationId(AnonymizeEmailPostProcessorFactory.class.getName(), "instance-0"), PluginState.ENABLED, 1, jsonNode));
    }

    @BeforeMethod(groups = { "receiverTests" })
    public void clearReceivedMessages() throws IOException {
        IntegrationTestRunner.clearMessages();
    }


    @AfterGroups(groups = { "receiverTests" })
    public void stopEmbeddedRts() throws IOException {
        IntegrationTestRunner.stop();
    }
}
