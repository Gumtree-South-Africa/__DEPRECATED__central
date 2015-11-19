package nl.marktplaats.integration.support;

import com.datastax.driver.core.Session;
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
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ReceiverTestsSetup {

    private static EmbeddedCassandra embeddedCassandra;

    @BeforeGroups(groups = { "receiverTests" })
    public void startEmbeddedRts() throws Exception {

        // WARNING!
        // This MUST happen first!
        // Do not put anything that creates a Logger before this line.
        // Don't you even dare to initialize fields with their definition!
        System.setProperty("confDir", configurationDirectory());

        //TODO: get key space from properties
        embeddedCassandra = new EmbeddedCassandra("replyts_integration_test");
        embeddedCassandra.start("/cassandra_schema.cql", "/cassandra_volume_filter_schema.cql");

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

        // Configure volume filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(VolumeFilterFactory.class.getName(), "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("[{'timeSpan': 10,'timeUnit': 'MINUTES','maxCount': 10,'score': 100}]")));
    }

    private String configurationDirectory() {
        // Current working directory is either the project root, or the replyts2-mp-integration-tests module root.
        // Find the configuration directory from both roots.
        List<String> configurationDirectoryAsModulePaths = Arrays.asList(
                "src/test/resources/mp-integration-test-conf",
                "replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf"
        );
        
        return configurationDirectoryAsModulePaths
                .stream()
                .filter(d -> new File(d).isDirectory())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Was not able to find configuration directory"));
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

    public Session getSession() {
        return embeddedCassandra.getSession();
    }
}
