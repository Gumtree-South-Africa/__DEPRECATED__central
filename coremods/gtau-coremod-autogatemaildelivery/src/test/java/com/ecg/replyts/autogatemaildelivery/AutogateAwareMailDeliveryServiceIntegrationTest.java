package com.ecg.replyts.autogatemaildelivery;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**-
 * @author mdarapour
 */
public class AutogateAwareMailDeliveryServiceIntegrationTest {
    private final static int HTTP_PORT = OpenPortFinder.findFreePort();
    private final static Logger LOGGER = LoggerFactory.getLogger(AutogateAwareMailDeliveryServiceIntegrationTest.class);

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        // Header Injector
        properties.put("replyts.header-injector.headers", "Http-Url,Http-Account-Name,Http-Account-Password");
        properties.put("replyts.header-injector.order", "250");
        // Autogate Delivery
        properties.put("replyts.autogate.header.url", "Http-Url");
        properties.put("replyts.autogate.header.account", "Http-Account-Name");
        properties.put("replyts.autogate.header.password", "Http-Account-Password");

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
        MailInterceptor.ProcessedMail mail = rule.deliver(MailBuilder.aNewMail().
                from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .plainBody("hello seller")
                .header("HTTP-URL", "http://localhost:" + HTTP_PORT)
                .header("Http-Account-Name".toUpperCase(), "test-account")
                .header("Http-Account-Password".toUpperCase(), "test-account"));

        rule.assertNoMailArrives();
        verify(1, postRequestedFor(urlMatching("/?(.*)")));
    }
}
