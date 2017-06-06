package com.ecg.replyts.integration.test;

import com.datastax.driver.core.Session;
import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.ecg.replyts.integration.test.support.Waiter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.DateTime;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.WiserMessage;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Junit test rule that starts an embedded ReplyTS instance, configured to communicate to a locally running dev
 * environment virtual box. This rule has convenience methods to inject mails into replyts and to block until the mail
 * was processed. <br/> If a message is not being processed in a configurable <code>deliveryTimeout</code>, the rule
 * will assume that the mail was lost and therefore fail. <h2>Usage</h2> The below example shows how to use that rule:
 * <pre>
 * public class SearchServiceTest {
 *
 * &#64;Rule public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule();
 *
 * &#64;Test public void rtsDoesNotProcessAutomatedMail() {
 *      MailInterceptor.ProcessedMail outcome =
 *      replyTsIntegrationTestRule.deliver(
 *          aNewMail()
 *          .from("a@b.com")
 *          .to("b@c.com")
 *          .adId("as")
 *          .header("precedence", "junk")
 *          .plainBody("foobar") );
 *
 *      assertEquals(ConversationState.ACTIVE, outcome.getConversation().getState());
 *      assertEquals(MessageState.IGNORED, outcome.getMessage().getState()); replyTsIntegrationTestRule.assertNoMailArrives();
 *  }
 *  }
 *  </pre>
 */
public class ReplyTsIntegrationTestRule implements TestRule {
    public static final boolean ES_ENABLED = true;
    private static final Logger LOG = LoggerFactory.getLogger(ReplyTsIntegrationTestRule.class);

    private int deliveryTimeoutSeconds;

    private String[] cqlFilePaths;

    private Description description;

    private ReplyTsConfigClient client;

    private IntegrationTestRunner testRunner;

    private CassandraIntegrationTestProvisioner CASDB = CassandraIntegrationTestProvisioner.getInstance();

    private String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private boolean cassandraEnabled = true;

    public ReplyTsIntegrationTestRule() {
        this(null, null, 20, false, new Class[0], "cassandra_schema.cql");
    }

    public ReplyTsIntegrationTestRule(boolean esEnabled) {
        this(null, null, 20, esEnabled, new Class[0], "cassandra_schema.cql");
    }

    public ReplyTsIntegrationTestRule(int deliveryTimeoutSeconds, boolean esEnabled) {
        this(null, null, deliveryTimeoutSeconds, esEnabled, new Class[0], "cassandra_schema.cql");
    }

    public ReplyTsIntegrationTestRule(Properties testProperties) {
        this(testProperties, null, 20, false, new Class[0], "cassandra_schema.cql");
    }

    public ReplyTsIntegrationTestRule(String replyTsConfigurationDir, String... cqlFilePaths) {
        this(null, replyTsConfigurationDir, 20, false, new Class[0], cqlFilePaths);
    }

    public ReplyTsIntegrationTestRule(Properties testProperties, String replyTsConfigurationDir, String... cqlFilePaths) {
        this(testProperties, replyTsConfigurationDir, 20, false, new Class[0], cqlFilePaths);
    }

    public ReplyTsIntegrationTestRule(Properties testProperties, String configurationResourceDirectory, int deliveryTimeoutSeconds, boolean esEnabled, String... cqlFilePaths) {
        this(testProperties, configurationResourceDirectory, deliveryTimeoutSeconds, esEnabled, new Class[0], cqlFilePaths);
    }

    public ReplyTsIntegrationTestRule(Properties properties, Class<?>... configuration) {
        this(properties, null, 20, false, configuration, "cassandra_schema.cql");
    }

    /**
     * instantiate a new rule, delivery timeout can be configured
     *
     * @param deliveryTimeoutSeconds maximum number of seconds {@link #deliver(MailBuilder)} should wait for a mail to
     *                               be processed.
     */
    public ReplyTsIntegrationTestRule(
            Properties testProperties, String configurationResourceDirectory, int deliveryTimeoutSeconds, boolean esEnabled, Class[] configuration, String... cqlFilePaths
    ) {
        this.deliveryTimeoutSeconds = deliveryTimeoutSeconds;
        this.cqlFilePaths = cqlFilePaths;

        if (testProperties == null) {
            testProperties = new Properties();
        }

        testProperties.put("persistence.cassandra.core.keyspace", keyspace);
        testProperties.put("persistence.cassandra.mb.keyspace", keyspace);
        testProperties.put("replyts.jetty.instrument", false);
        testProperties.put("mailreceiver.watch.retrydelay.millis", 250);
        testProperties.put("search.es.enabled", esEnabled);
        testProperties.put("replyts2-messagecenter-plugin.pushmobile.url", "UNSET_PROPERTY");
        testProperties.put("replyts2-messagecenter-plugin.api.host", "UNSET_PROPERTY");

        LOG.debug("Running tests with ES enabled: " + esEnabled);

        if ("riak".equals(testProperties.get("persistence.strategy"))) {
            cassandraEnabled = false;
        }

        if (cassandraEnabled) {
            testProperties.put("persistence.skip.mail.storage", true);
        }

        if (!testProperties.containsKey("replyts.tenant")) {
            testProperties.put("replyts.tenant", "unknown");
        }

        String configResourceDirectory = configurationResourceDirectory != null ? configurationResourceDirectory : ReplytsRunner.DEFAULT_CONFIG_RESOURCE_DIRECTORY;
        this.testRunner = new IntegrationTestRunner(testProperties, configResourceDirectory, configuration);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        this.description = description;

        Session session = null;

        if (cassandraEnabled) {
            try {
                session = CASDB.loadSchema(keyspace, cqlFilePaths);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        testRunner.start();

        Session finalSession = session;
        return new Statement() {
            @Override
            public void evaluate() throws Throwable { // NOSONAR
                client = new ReplyTsConfigClient(testRunner.getHttpPort());

                try {
                    testRunner.clearMessages();
                    base.evaluate();
                    testRunner.clearMessages();
                } finally {
                    cleanConfigs();
                    testRunner.stop();
                    if (cassandraEnabled) {
                        CASDB.cleanTables(finalSession, keyspace);
                    }
                }
            }
        };
    }

    public int getHttpPort() {
        return testRunner.getHttpPort();
    }

    private Client getSearchClient() {
        return testRunner.getSearchClient();
    }

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * registers a new filter/resultinspector config to the configapi, effectively starting up a new filter or result
     * inspector configuration. blocks until the configured service is up and running.
     */
    public Configuration.ConfigurationId registerConfig(Class<? extends BasePluginFactory> type, ObjectNode config) {
        return registerConfig(type, config, 100L);
    }


    /**
     * registers a new filter/resultinspector config to the configapi, effectively starting up a new filter or result
     * inspector configuration. blocks until the configured service is up and running.
     */
    public Configuration.ConfigurationId registerConfig(Class<? extends BasePluginFactory> type, ObjectNode config, long priority) {
        Configuration.ConfigurationId c = new Configuration.ConfigurationId(type.getName(), "instance-" + COUNTER.incrementAndGet());
        LOG.info("Created config " + c + " with priority " + priority);
        client.putConfiguration(new Configuration(c, PluginState.ENABLED, priority, config));
        try {
            // TODO kobyakov: the method does work as intended, but it's not used as intended in the tests:
            // a configuration is actually registered in comaas synchronously, by calling this method,
            // but an actual Plugin is created asynchronously by a notification via a hazelcast topic.
            // See ClusterRefreshPublisher/ClusterRefreshSubscriber. That has to be fixed in a nicer way, which is
            // almost any way except the following:
            Thread.sleep(378L);
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted while waiting for the configuration registration", e);
        }
        return c;
    }

    /**
     * unregisters a configuration, effectively switching that service off. blocks until that service is offline.
     */
    public void deleteConfig(Configuration.ConfigurationId c) {
        client.deleteConfiguration(c);
    }

    /**
     * remove all configurations to reset services to default state.
     */
    private void cleanConfigs() {
        for (Configuration c : client.listConfigurations()) {
            client.deleteConfiguration(c.getConfigurationId());
        }
    }

    /**
     * inputs a new mail into the dropfolder of the embedded ReplyTS instance and blocks until the mail has been fully
     * processed. the returned object referrs to the state of message and it's conversation after processing the mail
     * has completed.<br/> If the mail was not processed within the delivery timeout, a {@link RuntimeException} is
     * thrown.
     */
    public MailInterceptor.ProcessedMail deliver(MailBuilder mail) {
        String mailIdentifier = String.format("%s#%s.%s",
                description.getTestClass().getSimpleName(),
                description.getMethodName(),
                UUID.randomUUID().toString());

        LOG.info("Sending Mail with unique identifier '{}' at '{}'", mailIdentifier, DateTime.now());
        mail.uniqueIdentifier(mailIdentifier);
        File f = new File(testRunner.getDropFolder(), "tmp_pre_" + Math.random());

        try (FileOutputStream fout = new FileOutputStream(f)) {
            mail.write(fout);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            assert f.renameTo(new File(f.getParent(), "pre_" + f.getName()));
        }
        return testRunner.getMailInterceptor().awaitMailIdentifiedBy(mailIdentifier, deliveryTimeoutSeconds);
    }


    /**
     * ensures that replyTS does not (or has not) sent out any mail. has a very short delay, as the deliver method
     * blocks until replyts is done with that mail.
     */
    public void assertNoMailArrives() {
        // no need for high timeout. deliver mail already blocks until replyts has sent the mail out.
        testRunner.assertMessageDoesNotArrive(1, 100);
    }

    public ReplyTsConfigClient getConfigClient() {
        return client;
    }

    /**
     * awaits the next mail, ReplyTS sends out.
     */
    public MimeMessage waitForMail() {
        try {
            MimeMessage message = testRunner.waitForMessageArrival(1, deliveryTimeoutSeconds * 1000).getMimeMessage();
            testRunner.clearMessages();
            return message;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * awaits the next mail ReplyTS sends out.
     */
    public WiserMessage waitForWiserMail() {
        try {
            WiserMessage message = testRunner.waitForMessageArrival(1, deliveryTimeoutSeconds * 1000);
            testRunner.clearMessages();
            return message;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void waitUntilIndexedInEs(MailInterceptor.ProcessedMail mail) {
        Client searchClient = getSearchClient();
        String id = mail.getConversation().getId() + "/" + mail.getMessage().getId();
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch("replyts")
                .setTypes("message")
                .setQuery(QueryBuilders.termQuery("_id", id));

        Waiter.await(
                () -> searchClient.search(searchRequestBuilder.request()).actionGet().getHits().getTotalHits() > 0).
                within(10, TimeUnit.SECONDS);
    }

    public FileSystemMailSender getMailSender() {
        return testRunner.getMailSender();
    }

    public MailInterceptor getMailInterceptor() {
        return testRunner.getMailInterceptor();
    }

    public ReplyTsIntegrationTestRule addCassandraSchema(String cqlFile) {
        List<String> tmp = new ArrayList<>(Arrays.asList(cqlFilePaths));
        tmp.add(cqlFile);
        cqlFilePaths = tmp.toArray(cqlFilePaths);
        return this;
    }
}
