package nl.marktplaats.integration.support;

import com.ecg.replyts.integration.test.IntegrationTestRunner;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;

public class ReceiverTestsSetup {

    @BeforeGroups(groups = { "receiverTests" })
    public void startEmbeddedRts() throws IOException {
        System.setProperty("confDir", "/Users/evanoosten/dev/mp/replyts2/mp-replyts2/replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf");
        IntegrationTestRunner.setConfigResourceDirectory("/mp-integration-test-conf");
        IntegrationTestRunner.getReplytsRunner();

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

}
