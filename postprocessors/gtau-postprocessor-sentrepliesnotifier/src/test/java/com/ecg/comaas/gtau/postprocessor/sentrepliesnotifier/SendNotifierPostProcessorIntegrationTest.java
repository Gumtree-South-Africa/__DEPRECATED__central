package com.ecg.comaas.gtau.postprocessor.sentrepliesnotifier;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author mdarapour
 */
public class SendNotifierPostProcessorIntegrationTest {

    private final static int DEFAULT_PORT       = Integer.parseInt(System.getProperty("replyts.sentnotifier.filter.test.port","12345"));
    private final static Logger LOGGER = LoggerFactory.getLogger(SendNotifierPostProcessorIntegrationTest.class);

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DEFAULT_PORT);

    @BeforeClass
    public static void load() {
        System.setProperty("replyts.sendnotifier.endpoint.url", String.format("http://localhost:%d/",DEFAULT_PORT));
    }

    @AfterClass
    public static void down() {
        try {
            WireMock.shutdownServer();
        } catch(Exception ex) {
            LOGGER.warn("Perhaps WireMock has already gone down!", ex);
        }
    }

    @Test
    public void initialBuyerToSellerConversation() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("hello seller"));
        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213")));
    }

    @Test
    public void xmlMessage() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        verify(0, getRequestedFor(urlMatching("/?(.*)")));
        verify(0, getRequestedFor(urlEqualTo("/?adId=213")));
    }

    @Test
    public void replySellerToBuyer() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("hello seller"));
        MimeMessage anonymizedInitialMail = rule.waitForMail();
        String anonymizedBuyer = anonymizedInitialMail.getFrom()[0].toString();
        rule.deliver(MailBuilder.aNewMail().from("seller@bar.com").to(anonymizedBuyer).adId("213").htmlBody("hello buyer"));
        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213")));
    }

    @Test
    public void secondReplyBuyerToSeller() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("hello seller"));
        MimeMessage anonymizedInitialMail = rule.waitForMail();
        String anonymizedBuyer = anonymizedInitialMail.getFrom()[0].toString();
        rule.deliver(MailBuilder.aNewMail().from("seller@bar.com").to(anonymizedBuyer).adId("213").htmlBody("hello buyer"));
        MimeMessage anonymizedSecondMail = rule.waitForMail();
        String anonymizedSeller = anonymizedSecondMail.getFrom()[0].toString();
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to(anonymizedSeller).adId("213").htmlBody("bye seller"));
        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213")));
    }
}
