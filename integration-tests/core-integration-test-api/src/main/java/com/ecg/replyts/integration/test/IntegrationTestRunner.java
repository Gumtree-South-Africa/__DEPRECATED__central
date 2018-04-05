package com.ecg.replyts.integration.test;

import io.prometheus.client.CollectorRegistry;
import org.elasticsearch.client.Client;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.WiserMessage;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class IntegrationTestRunner extends ExternalResource {
    private final Logger LOGGER = LoggerFactory.getLogger("IntegrationTestRunner");

    private Properties testProperties;

    private String configResourceDirectory;

    private ReplytsRunner replytsRunner;

    private FileSystemMailSender mailSender;

    private Boolean isRunning = false;
    private Class<?>[] configurations;

    public IntegrationTestRunner(Properties testProperties, String configResourceDirectory, Class<?> ... configurations) {
        this.testProperties = testProperties;
        this.configResourceDirectory = configResourceDirectory;
        this.configurations = configurations;
    }

    public void start() {
        if (isRunning)
            throw new IllegalStateException("COMaaS is already running - won't be started multiple times");

        try {
            LOGGER.info("Starting COMaaS");

            replytsRunner = new ReplytsRunner(testProperties, configResourceDirectory, configurations);
            mailSender = new FileSystemMailSender(replytsRunner.getDropFolder());

            isRunning = true;
        } catch (Exception e) {
            LOGGER.error("COMaaS startup failed for integration test.", e);

            throw new IllegalStateException("Failed to start COMaaS", e);
        }
    }

    public void stop() {
        if (isRunning) {
            replytsRunner.stop();

            isRunning = false;

            LOGGER.info("Stopped COMaaS");

            CollectorRegistry.defaultRegistry.clear();
        }
    }

    public int getHttpPort() {
        ensureStarted();

        return replytsRunner.getHttpPort();
    }

    public Client getSearchClient() {
        ensureStarted();

        return replytsRunner.getSearchClient();
    }

    public File getDropFolder() {
        ensureStarted();

        return replytsRunner.getDropFolder();
    }

    public FileSystemMailSender getMailSender() {
        ensureStarted();

        return mailSender;
    }

    public void clearMessages() {
        ensureStarted();

        replytsRunner.getMessages().clear();
    }

    public MailInterceptor getMailInterceptor() {
        return replytsRunner.getMailInterceptor();
    }

    /* Use rule.waitForMail() instead */
    public WiserMessage waitForMessageArrival(int expectedEmailNumber, long timeout) throws Exception {
        ensureStarted();

        long deadline = System.currentTimeMillis() + timeout;
        List<WiserMessage> messages;
        int loopCounter = 0;
        do {
            loopCounter++;
            TimeUnit.MILLISECONDS.sleep(100);
            messages = replytsRunner.getMessages();
            LOGGER.debug("Messages after {} milliseconds: {}", 10 * loopCounter, messages.size());
        } while (messages.size() < expectedEmailNumber && System.currentTimeMillis() < deadline);

        if (messages.size() != expectedEmailNumber) {
            Assert.fail("Expected " + expectedEmailNumber + " mails to arrive, but got " + messages.size());
        }
        return messages.get(messages.size() - 1);
    }

    public void assertMessageDoesNotArrive(int expectedEmailNumber, long timeout) {
        List<WiserMessage> messages = replytsRunner.getMessages();
        try {
            ensureStarted();

            long deadline = System.currentTimeMillis() + timeout;
            Thread.sleep(10);

            while (messages.size() < expectedEmailNumber && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
                messages = replytsRunner.getMessages();
                assertThat("number of arrived messages", messages.size(), lessThan(expectedEmailNumber));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertThat("number of arrived messages", messages.size(), lessThan(expectedEmailNumber));
    }

    public WiserMessage getRtsSentMail(int emailNumber) {
        ensureStarted();

        List<WiserMessage> messages = replytsRunner.getMessages();
        assertThat("number of arrived messages", messages.size(), greaterThanOrEqualTo(emailNumber));
        return messages.get(emailNumber - 1);
    }

    public WiserMessage getLastRtsSentMail() {
        ensureStarted();

        List<WiserMessage> messages = replytsRunner.getMessages();
        assertThat("number of arrived messages", messages.size(), greaterThan(0));
        return messages.get(messages.size() - 1);
    }

    private void ensureStarted() {
        if (!isRunning) {
            throw new IllegalStateException("COMaaS is not currently running");
        }
    }
}
