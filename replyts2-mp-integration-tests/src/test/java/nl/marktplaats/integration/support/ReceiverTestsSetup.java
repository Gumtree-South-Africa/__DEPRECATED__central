package nl.marktplaats.integration.support;

import com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector.ThresholdResultInspectorFactory;
import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.marktplaats.filter.bankaccount.BankAccountFilterFactory;
import nl.marktplaats.filter.knowngood.KnownGoodFilterFactory;
import nl.marktplaats.filter.volume.VolumeFilterFactory;
import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessor;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;

public class ReceiverTestsSetup {

    //TODO: get key space from properties
    private static EmbeddedCassandra embeddedCassandra = new EmbeddedCassandra("replyts_integration_test");

    @BeforeGroups(groups = { "receiverTests" })
    public void startEmbeddedRts() throws Exception {

        embeddedCassandra.start("/cassandra_schema.cql", "/cassandra_volume_filter_schema.cql");

        // TODO: fix confDir hack on next line, it should be picked up by IntegrationTestRunner.setConfigResourceDirectory
        System.setProperty("confDir", "/Users/evanoosten/dev/mp/replyts2/mp-replyts2/replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf");
        IntegrationTestRunner.setConfigResourceDirectory("/mp-integration-test-conf");
        IntegrationTestRunner.getReplytsRunner();

        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(IntegrationTestRunner.getReplytsRunner().getReplytsHttpPort());

        // Configure result inspector
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(ThresholdResultInspectorFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("{'held':50, 'blocked':100}")));

        // Configure known good filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(KnownGoodFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        new ObjectMapper().createObjectNode()));

        // Configure bank account filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(BankAccountFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("{'fraudulentBankAccounts': ['123456', '987654321', '87238935']}")));


        String volumeFilterConfig = "[" +
                "{" +
                "\"timeSpan\":10," +
                "\"timeUnit\":\"MINUTES\"," +
                "\"maxCount\":10," +
                "\"score\":50" +
                "}" +
                "]";

        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(VolumeFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse(volumeFilterConfig)));


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
        embeddedCassandra.cleanEmbeddedCassandra();
    }
}
