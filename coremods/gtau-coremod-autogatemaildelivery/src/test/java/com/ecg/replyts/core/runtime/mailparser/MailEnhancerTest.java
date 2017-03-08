package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import org.mockito.internal.util.reflection.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.WiserMessage;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.Assert.*;


public class MailEnhancerTest {
    private final static int HTTP_PORT = OpenPortFinder.findFreePort();
    private final static Logger LOGGER = LoggerFactory.getLogger(MailEnhancerTest.class);

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        // Header Injector
        properties.put( "replyts.header-injector.headers", "Http-Url,Http-Account-Name,Http-Account-Password,X-Reply-Channel,Email-From-Override,Email-To-Override,Email-Cc-Override,Email-Bcc-Override" );
        properties.put( "replyts.header-injector.order", "250" );
        // Autogate Delivery
        properties.put( "replyts.header.email.from.list", "Email-From-Override" );
        properties.put( "replyts.header.email.to.list", "Email-To-Override" );
        properties.put( "replyts.header.email.cc.list", "Email-Cc-Override" );
        properties.put( "replyts.header.email.bcc.list", "Email-Bcc-Override" );

        return properties;
    }).get());


    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(HTTP_PORT);

    @Before
    public void setup() {
        WireMock.resetToDefault();
        WireMock.resetAllScenarios();
    }

    @AfterClass
    public static void down() {
        try{
            WireMock.shutdownServer();
        } catch(Exception ex) {
            LOGGER.warn("Perhaps WireMock has already gone down!", ex);
        }
    }

    @Test
    public void verifyNormalEmails() throws MessagingException {
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("hello seller"));
        MimeMessage mail = rule.waitForMail();
        assertNotEquals("buyer@foo.com", mail.getHeader("From", ","));
        assertEquals("seller@bar.com", mail.getHeader("To", ","));
        assertNull(mail.getHeader("Cc", ","));
        assertNull(mail.getHeader("Bcc", ","));
    }

    @Test
    public void verifyOverrideEmailsSingleList() throws Exception {
        MailBuilder mailToSend = MailBuilder.aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")
                .header("Email-From-Override", "from@gumtree.com")
                .header("Email-To-Override", "to@gumtree.com")
                .header("Email-Cc-Override", "cc@gumtree.com")
                .header("Email-Bcc-Override", "bcc@gumtree.com");
        rule.deliver(mailToSend);
        IntegrationTestRunner testRunner = (IntegrationTestRunner) Whitebox.getInternalState(this.rule, "testRunner");
        // Wait for mail returns error as more than one messages received due to To and Cc list, hence below method
        MimeMessage mail = testRunner.waitForMessageArrival(3, 1000).getMimeMessage();
        assertEquals("from@gumtree.com", mail.getHeader("From", ","));
        assertEquals("to@gumtree.com", mail.getHeader("To", ","));
        assertEquals("cc@gumtree.com", mail.getHeader("Cc", ","));
        assertNull(mail.getHeader("Email-From-Override"));
        assertNull(mail.getHeader("Email-To-Override"));
        assertNull(mail.getHeader("Email-Cc-Override"));
        assertNull(mail.getHeader("Email-Bcc-Override"));

        // BCC does not appear
        testRunner.clearMessages();
        rule.deliver(mailToSend);
        WiserMessage message = testRunner.getRtsSentMail(3);
        assertEquals("bcc@gumtree.com", message.getEnvelopeReceiver());
    }

    @Test
    public void verifyOverrideEmailsMultipleMessages() throws Exception {
        MailBuilder mailToSend = MailBuilder.aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")
                .header("Email-From-Override", "from@gumtree.com")
                .header("Email-To-Override", "to1@gumtree.com, to2@gumtree.com")
                .header("Email-Cc-Override", "cc1@gumtree.com, cc2@gumtree.com")
                .header("Email-Bcc-Override", "bcc1@gumtree.com, bcc2@gumtree.com");
        rule.deliver(mailToSend);
        IntegrationTestRunner testRunner = (IntegrationTestRunner) Whitebox.getInternalState(this.rule, "testRunner");
        // Wait for mail returns error as more than one messages received due to To and Cc list, hence below method
        MimeMessage mail = testRunner.waitForMessageArrival(6, 1000).getMimeMessage();
        assertEquals("from@gumtree.com", mail.getHeader("From", ","));
        assertEquals("to1@gumtree.com, to2@gumtree.com", mail.getHeader("To", ","));
        assertEquals("cc1@gumtree.com, cc2@gumtree.com", mail.getHeader("Cc", ","));
        // Verify that the headers are removed
        assertNull(mail.getHeader("Email-From-Override"));
        assertNull(mail.getHeader("Email-To-Override"));
        assertNull(mail.getHeader("Email-Cc-Override"));
        assertNull(mail.getHeader("Email-Bcc-Override"));

        // BCC does not appear
        testRunner.clearMessages();
        rule.deliver(mailToSend);
        WiserMessage message = testRunner.getRtsSentMail(5);
        assertEquals("bcc1@gumtree.com", message.getEnvelopeReceiver());
        testRunner.clearMessages();
        rule.deliver(mailToSend);
        message = testRunner.getRtsSentMail(6);
        assertEquals("bcc2@gumtree.com", message.getEnvelopeReceiver());
    }
}
