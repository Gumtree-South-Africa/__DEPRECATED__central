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
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

@Test
public class ReceiverTestsSetup {
    private String KEYSPACE = EmbeddedCassandra.createUniqueKeyspaceName();

    private EmbeddedCassandra embeddedCassandra = EmbeddedCassandra.getInstance();

    private Session session;

    protected IntegrationTestRunner runner;

    @BeforeGroups(groups = { "receiverTests" })
    public void startEmbeddedRts() throws Exception {
        session = embeddedCassandra.loadSchema(KEYSPACE, "cassandra_schema.cql", "cassandra_volume_filter_schema.cql");

        runner = new IntegrationTestRunner(((Supplier<Properties>) () -> {
            Properties properties = new Properties();

            properties.put("confDir", ((Supplier<String>) () -> {
                List<String> configurationDirectoryAsModulePaths = Arrays.asList(
                        "src/test/resources/mp-integration-test-conf",
                        "replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf"
                );

                return configurationDirectoryAsModulePaths
                        .stream()
                        .filter(d -> new File(d).isDirectory())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Was not able to find configuration directory"));
            }).get());
            properties.put("persistence.cassandra.keyspace", KEYSPACE);

            return properties;
        }).get(), "/mp-integration-test-conf");

        runner.start();

        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(runner.getHttpPort());

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

    @BeforeMethod(groups = { "receiverTests" })
    public void clearReceivedMessages() throws IOException {
        runner.clearMessages();
    }

    @AfterGroups(groups = { "receiverTests" })
    public void stopEmbeddedRts() throws IOException {
        runner.stop();

        embeddedCassandra.cleanTables(session, KEYSPACE);
    }

    public Session getSession() {
        return session;
    }
}
