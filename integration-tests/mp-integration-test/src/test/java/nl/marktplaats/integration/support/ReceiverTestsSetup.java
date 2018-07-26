package nl.marktplaats.integration.support;

import com.datastax.driver.core.Session;
import com.ecg.comaas.core.resultinspector.threshold.ThresholdResultInspectorFactory;
import com.ecg.comaas.mp.filter.bankaccount.BankAccountFilterFactory;
import com.ecg.comaas.mp.filter.volume.VolumeFilterFactory;
import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.util.Properties;
import java.util.stream.Stream;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

public class ReceiverTestsSetup {
    private static String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private static CassandraIntegrationTestProvisioner embeddedCassandra = CassandraIntegrationTestProvisioner.getInstance();

    private static Session session;

    protected static IntegrationTestRunner runner = null;

    @BeforeGroups(groups = { "receiverTests" })
    public static void startEmbeddedRts() {
        if (runner != null) {
            throw new IllegalStateException("IntegrationTestRunner has already been set up - should only happen once for this group of tests!");
        }

        session = embeddedCassandra.loadSchema(KEYSPACE, "cassandra_schema.cql", "cassandra_volume_filter_schema.cql");

        runner = new IntegrationTestRunner(createProperties(), "/mp-integration-test-conf");

        runner.start();

        ReplyTsConfigClient replyTsConfigClient = new ReplyTsConfigClient(runner.getHttpPort());

        // Configure result inspector
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(ThresholdResultInspectorFactory.IDENTIFIER, "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("{'held':50, 'blocked':100}")));

        // Configure bank account filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(BankAccountFilterFactory.IDENTIFIER, "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("{'fraudulentBankAccounts': ['123456', '987654321', '87238935']}")));

        // Configure volume filter
        replyTsConfigClient.putConfiguration(
                new Configuration(
                        new Configuration.ConfigurationId(VolumeFilterFactory.IDENTIFIER, "instance-0"),
                        PluginState.ENABLED,
                        1,
                        JsonObjects.parse("[{'timeSpan': 10,'timeUnit': 'MINUTES','maxCount': 10,'score': 100}]")));
    }

    private static Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_MP);

        properties.put("confDir", createConfDir());
        properties.put("persistence.cassandra.core.keyspace", KEYSPACE);
        properties.put("persistence.cassandra.mb.keyspace", KEYSPACE);
        properties.put("replyts.tenant.short", "unknown");

        return properties;
    }

    private static String createConfDir() {
        return Stream
                .of("src/test/resources/mp-integration-test-conf",
                    "replyts2-mp-integration-tests/src/test/resources/mp-integration-test-conf")
                .filter(d -> new File(d).isDirectory()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Was not able to find configuration directory"));
    }

    @BeforeMethod(groups = { "receiverTests" })
    public static void clearReceivedMessages() {
        runner.clearMessages();
    }

    @AfterGroups(groups = { "receiverTests" })
    public static void stopEmbeddedRts() {
        runner.stop();

        embeddedCassandra.cleanTables(session, KEYSPACE);
    }

    protected Session getSession() {
        return session;
    }
}
