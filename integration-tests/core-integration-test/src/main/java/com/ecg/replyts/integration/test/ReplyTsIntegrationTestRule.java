package com.ecg.replyts.integration.test;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.ReplyTsConfigClient;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.common.joda.time.DateTime;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ecg.replyts.integration.riak.EmbeddedRiakClientConfiguration.resetBrain;
import static com.ecg.replyts.integration.test.IntegrationTestRunner.assertMessageDoesNotArrive;
import static com.ecg.replyts.integration.test.IntegrationTestRunner.clearMessages;
import static com.ecg.replyts.integration.test.IntegrationTestRunner.getReplytsRunner;
import static com.ecg.replyts.integration.test.IntegrationTestRunner.setConfigResourceDirectory;
import static com.ecg.replyts.integration.test.IntegrationTestRunner.waitForMessageArrival;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
 *      AwaitMailSentProcessedListener.ProcessedMail outcome =
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplyTsIntegrationTestRule.class);
    private final int deliveryTimeoutSeconds;
    private final String[] cqlFilePaths;

    private Description description;
    private ReplyTsConfigClient client;
    private String replyTsConfigurationDir;
    private EmbeddedCassandra embeddedCassandra;

    /**
     * instantiate new rule, with a default delivery timeout of 5 seconds.
     */
    public ReplyTsIntegrationTestRule() {
        this(5, "/cassandra_schema.cql");
    }

    public ReplyTsIntegrationTestRule(String replyTsConfigurationDir, String... cqlFilePaths) {
        this(5, cqlFilePaths);
        this.replyTsConfigurationDir = replyTsConfigurationDir;
    }

    /**
     * instantiate a new rule, delivery timeout can be configured
     *
     * @param deliveryTimeoutSeconds maximum number of seconds {@link #deliver(MailBuilder)} should wait for a mail to
     *                               be processed.
     */
    private ReplyTsIntegrationTestRule(int deliveryTimeoutSeconds, String... cqlFilePaths) {
        this.deliveryTimeoutSeconds = deliveryTimeoutSeconds;
        this.cqlFilePaths = cqlFilePaths;
        this.embeddedCassandra = new EmbeddedCassandra("replyts_integration_test");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        this.description = description;
        try {
            embeddedCassandra.start(cqlFilePaths);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable { // NOSONAR
                if (isNotBlank(replyTsConfigurationDir)) {
                    setConfigResourceDirectory(replyTsConfigurationDir);
                }
                ReplytsRunner runner = getReplytsRunner();
                client = new ReplyTsConfigClient(runner.getReplytsHttpPort());

                try {
                    resetBrain();
                    clearMessages();
                    base.evaluate();
                    clearMessages();
                } finally {
                    cleanConfigs();
                    IntegrationTestRunner.stop();
                    embeddedCassandra.clean();
                }
            }
        };
    }

    /**
     * returns the port the screening and config api are running on. (RESTful API)
     */
    public int getHttpPort() {
        return getReplytsRunner().getReplytsHttpPort();
    }

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * registers a new filter/resultinspector config to the configapi, effectively starting up a new filter or result
     * inspector configuration. blocks until the configured service is up and running.
     */
    public Configuration.ConfigurationId registerConfig(Class<? extends BasePluginFactory> type, ObjectNode config) {
        Configuration.ConfigurationId c = new Configuration.ConfigurationId(type.getName(), "instance-" + COUNTER.incrementAndGet());
        LOGGER.info("Created config " + c);
        client.putConfiguration(new Configuration(c, PluginState.ENABLED, 100l, config));
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
    public AwaitMailSentProcessedListener.ProcessedMail deliver(MailBuilder mail) {
        String mailIdentifier = String.format("%s#%s.%s",
                description.getTestClass().getSimpleName(),
                description.getMethodName(),
                UUID.randomUUID().toString());

        LOGGER.info("Sending Mail with unique identifier '{}' at '{}'", mailIdentifier, DateTime.now());
        mail.uniqueIdentifier(mailIdentifier);
        File f = new File(getReplytsRunner().getDropFolder(), "tmp_pre_" + Math.random());

        try (FileOutputStream fout = new FileOutputStream(f)) {
            mail.write(fout);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            f.renameTo(new File(f.getParent(), "pre_" + f.getName()));
        }
        return AwaitMailSentProcessedListener.awaitMailIdentifiedBy(mailIdentifier, deliveryTimeoutSeconds);
    }


    /**
     * ensures that replyTS does not (or has not) sent out any mail. has a very short delay, as the deliver method
     * blocks until replyts is done with that mail.
     */
    public void assertNoMailArrives() {
        // no need for high timeout. deliver mail already blocks until replyts has sent the mail out.
        assertMessageDoesNotArrive(1, 100);
    }

    public ReplyTsConfigClient getConfigClient() {
        return client;
    }

    /**
     * awaits the next mail, ReplyTS sends out.
     */
    public MimeMessage waitForMail() {
        try {
            MimeMessage message = waitForMessageArrival(1, 1000).getMimeMessage();
            clearMessages();
            return message;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
