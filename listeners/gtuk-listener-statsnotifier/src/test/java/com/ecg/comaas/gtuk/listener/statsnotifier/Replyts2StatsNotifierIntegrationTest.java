package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@Ignore
public class Replyts2StatsNotifierIntegrationTest {
    private final static int DEFAULT_TIMEOUT    = 10000;
    private final static int TEST_TIMEOUT       = 30000;
    private final static int DEFAULT_PORT       = OpenPortFinder.findFreePort();

    private final Properties testProperties = new Properties() {{
        put("gumtree.stats.api.url", String.format("http://localhost:%d/stats-api",DEFAULT_PORT));
        put("gumtree.analytics.ga.trackingid", "UA-123456");
        put("gumtree.analytics.ga.host", String.format("http://localhost:%d/google-analytics", DEFAULT_PORT));
        put("gumtree.analytics.ga.enabled", "true");
    }};

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(testProperties, null, DEFAULT_TIMEOUT, false);

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(DEFAULT_PORT);

    @AfterClass
    public static void down() {
        try {
            WireMock.shutdownServer();
        } catch (Exception e) {
            // Perhaps WireMock has already gone down!
        }
    }

    @Test(timeout = TEST_TIMEOUT)
    public void sendAdIdToStatsAndGA() throws Exception {
        stubFor(post(urlMatching("/stats-api/advert-stats/(.*)/counts/reply")).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlMatching("/google-analytics?v=1&t=event&tid=UA-123456&ea=ReplyEmailBackendSuccess&ec=ReplySuccess&cid=abcd.efgh"))
                .willReturn(aResponse().withStatus(200)));
        MailInterceptor.ProcessedMail mail =
                rule.deliver(MailBuilder
                        .aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Clientid", "abcd.efgh"));

        Awaitility.await().atMost(TEST_TIMEOUT, MILLISECONDS).
                pollInterval(1000, MILLISECONDS).until(() -> MessageState.SENT == mail.getMessage().getState());
        assertThat(MessageState.SENT, equalTo(mail.getMessage().getState()));

        verify(1, postRequestedFor(urlEqualTo("/stats-api/advert-stats/213/counts/reply")).
                withHeader("Content-Type", equalTo("application/x-www-form-urlencoded")).
                withRequestBody(equalTo("from=buyer%40foo.com")));
        verify(1, postRequestedFor(
                urlEqualTo("/google-analytics?v=1&t=event&tid=UA-123456&ea=ReplyEmailBackendSuccess&ec=ReplySuccess&cid=abcd.efgh")));
    }
}