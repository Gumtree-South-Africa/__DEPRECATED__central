package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class EchelonFilterIntegrationTest {

    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int DEFAULT_PORT = Integer.parseInt(System.getProperty("replyts.echelon.filter.test.port", String.valueOf(OpenPortFinder.findFreePort())));
    private static final String RESPONSE_OK = "OK";
    private static final String KO_FEEDBACK = "Feedback";
    private static final String RESPONSE_KO = "KO\n" + KO_FEEDBACK;

    @Rule
    public ReplyTsIntegrationTestRule rtsRule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_GTAU));

    @Rule
    public WireMockClassRule wireMockServer = new WireMockClassRule(DEFAULT_PORT);

    @Before
    public void setup() {
        rtsRule.registerConfig(EchelonFilterFactory.IDENTIFIER,
                (ObjectNode) JsonObjects.parse(
                        String.format("{endpointUrl:'http://localhost:%d/',endpointTimeout:%d,score:0}", DEFAULT_PORT, DEFAULT_TIMEOUT)));
    }

    @Test
    public void testRequestDone() {
        stubFor(get(urlMatching(".*adId=123.*")).willReturn(aResponse().withStatus(200).withBody(RESPONSE_OK.getBytes())));

        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("123")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(".*")));
        assertThat(requests.size(), is(1));
        verify(1, getRequestedFor(urlEqualTo("/?adId=123&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));
        assertEquals("FilterFeedback", 0, processedMail.getMessage().getProcessingFeedback().size());
    }

    @Test
    public void testNegativeResponse() {
        stubFor(get(urlMatching(".*adId=124.*")).willReturn(aResponse().withStatus(200).withBody(RESPONSE_KO.getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("124")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(".*")));
        assertThat(requests.size(), is(1));
        verify(1, getRequestedFor(urlEqualTo("/?adId=124&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        Assert.assertNotNull(result);
        Assert.assertFalse("Processing Feedback must not be empty", result.isEmpty());
        ProcessingFeedback reason = result.get(0);
        Assert.assertEquals(KO_FEEDBACK, reason.getDescription());
        Assert.assertEquals(FilterResultState.DROPPED, reason.getResultState());
    }

    @Test
    public void testEmptyResponse() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200).withBody("".getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testSilentNegativeResponse() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200).withBody("KO".getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        ProcessingFeedback feedback = result.get(0);
        Assert.assertEquals("", feedback.getDescription());
        Assert.assertEquals(FilterResultState.DROPPED, feedback.getResultState());
    }

    @Test
    public void testUnexpectedResponse() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200).withBody("Unexpected string".getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testErrorResponse() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(500).withBody("ERROR".getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testForbiddenResponse() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(403).withBody("".getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "machineId")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=machineId&categoryId=1&email=buyer%40foo.com")));
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testIgnoredUnknownMachineId() throws Exception {
        stubFor(get(urlMatching("/?(.*)")).willReturn(aResponse().withStatus(200).withBody(RESPONSE_OK.getBytes())));
        MailInterceptor.ProcessedMail processedMail =
                rtsRule.deliver(MailBuilder.aNewMail()
                        .from("buyer@foo.com")
                        .to("seller@bar.com")
                        .adId("213")
                        .htmlBody("hello seller")
                        .customHeader("Ip", "127.0.0.1")
                        .customHeader("Mach-Id", "unknown")
                        .customHeader("Categoryid", "1"));

        assertNotNull(processedMail);
        List<ProcessingFeedback> result = processedMail.getMessage().getProcessingFeedback();

        verify(1, getRequestedFor(urlMatching("/?(.*)")));
        verify(1, getRequestedFor(urlEqualTo("/?adId=213&ip=127.0.0.1&machineId=&categoryId=1&email=buyer%40foo.com")));
    }
}
