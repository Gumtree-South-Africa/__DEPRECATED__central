package com.ecg.replyts.integration.test;

import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.WiserMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Runs tests that depend on a mail receiver.
 */
public class IntegrationTestRunner extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger("IntegrationTestRunner");
    private static ReplytsRunner replytsRunner;
    private static FileSystemMailSender mailSender;
    private static String configResourceDirectory = ReplytsRunner.DEFAULT_CONFIG_RESOURCE_DIRECTORY;

    /**
     * Sets the config resource directory.
     *
     * !!   MUST BE CALLED BEFORE ANY OTHER METHOD IN THIS CLASS  !!
     *
     * @param configResourceDirectory the classpath directory of the configuration files to use
     */
    public static void setConfigResourceDirectory(String configResourceDirectory) {
        IntegrationTestRunner.configResourceDirectory = configResourceDirectory;
    }

    public static void start() {
        try {
            LOGGER.info("Starting Reply T&S");
            replytsRunner = new ReplytsRunner(configResourceDirectory);
            replytsRunner.start();

            mailSender = new FileSystemMailSender(replytsRunner.getDropFolder());
        } catch (Exception e) {
            LOGGER.error("ReplyTS startup failed for integration test.", e);
            throw new IllegalStateException("Failed to start Reply T&S", e);
        }
    }

    public static void stop() {
        if(replytsRunner != null) {
            replytsRunner.stop();
            replytsRunner = null;
            LOGGER.info("Stopped Reply T&S");
        }
    }

    public static ReplytsRunner getReplytsRunner() {
        checkStarted();
        return replytsRunner;
    }

    public static FileSystemMailSender getMailSender() {
        checkStarted();
        return mailSender;
    }

    public static void clearMessages() {
        checkStarted();
        replytsRunner.getMessages().clear();
    }

    public static WiserMessage waitForMessageArrival(int expectedEmailNumber, long timeout) throws Exception {
        checkStarted();

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
        // assertThat("number of arrived messages", messages.size(), is(expectedEmailNumber));
        return messages.get(messages.size() - 1);
    }

    public static void assertMessageDoesNotArrive(int expectedEmailNumber, long timeout) {
        List<WiserMessage> messages = replytsRunner.getMessages();
        try {
            checkStarted();
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

    public static WiserMessage getRtsSentMail(int emailNumber) {
        checkStarted();
        List<WiserMessage> messages = replytsRunner.getMessages();
        assertThat("number of arrived messages", messages.size(), greaterThanOrEqualTo(emailNumber));
        return messages.get(emailNumber - 1);
    }

    public static WiserMessage getLastRtsSentMail() {
        checkStarted();
        List<WiserMessage> messages = replytsRunner.getMessages();
        assertThat("number of arrived messages", messages.size(), greaterThan(0));
        return messages.get(messages.size() - 1);
    }

    private static void checkStarted() {
        if (replytsRunner == null) {
            start();
        }
    }

}
