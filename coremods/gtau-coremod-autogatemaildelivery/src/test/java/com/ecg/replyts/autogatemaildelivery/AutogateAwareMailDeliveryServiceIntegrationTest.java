package com.ecg.replyts.autogatemaildelivery;

import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertNotNull;

/**
 * @author mdarapour
 */
public class AutogateAwareMailDeliveryServiceIntegrationTest {
    private final static int DEFAULT_PORT       = 12347;
    private final static Logger LOGGER = LoggerFactory.getLogger(AutogateAwareMailDeliveryServiceIntegrationTest.class);

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(DEFAULT_PORT);

    @BeforeClass
    public static void load() {
        // Header Injector
        System.setProperty( "replyts.header-injector.headers", "Http-Url,Http-Account-Name,Http-Account-Password" );
        System.setProperty( "replyts.header-injector.order", "250" );
        // Autogate Delivery
        System.setProperty( "replyts.autogate.header.url", "Http-Url" );
        System.setProperty( "replyts.autogate.header.account", "Http-Account-Name" );
        System.setProperty( "replyts.autogate.header.password", "Http-Account-Password" );
    }

    @Before
    public void setup() {
        WireMock.resetToDefault();
        WireMock.resetAllScenarios();
    }

    @AfterClass
    public static void down() {
        try {
            WireMock.shutdownServer();
        } catch(Exception ex) {
            LOGGER.warn("Perhaps WireMock has already gone down!", ex.getMessage());
        }
    }

    @Test
    public void ignoresNormalEmails() {
        stubFor(post(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        rule.deliver(MailBuilder.aNewMail().from("buyer@foo.com").to("seller@bar.com").adId("213").htmlBody("hello seller"));
        MimeMessage mail = rule.waitForMail();
        verify(0, getRequestedFor(urlMatching("/?(.*)")));
    }
    //FIXME
    @Ignore
    public void redirectsMailsToAutogate() throws Exception {
        stubFor(post(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200)));
        AwaitMailSentProcessedListener.ProcessedMail mail = rule.deliver(MailBuilder.aNewMail().
                from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .plainBody("hello seller")
                .header("HTTP-URL", "http://localhost:" + DEFAULT_PORT)
                .header("Http-Account-Name".toUpperCase(), "test-account")
                .header("Http-Account-Password".toUpperCase(), "test-account"));

        rule.assertNoMailArrives();
        verify(1, postRequestedFor(urlMatching("/?(.*)")));
    }
}
