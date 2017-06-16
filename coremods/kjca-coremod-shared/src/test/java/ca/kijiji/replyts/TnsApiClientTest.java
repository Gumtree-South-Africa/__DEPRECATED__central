package ca.kijiji.replyts;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertEquals;


public class TnsApiClientTest {
    @ClassRule
    public static WireMockClassRule wireMockRule1 = new WireMockClassRule(0);

    private TnsApiClient tnsApiClient;

    private ResponseDefinitionBuilder statusOkResponse;
    private ResponseDefinitionBuilder delayedResponse;
    private MappingBuilder postRequest;
    private String adId = "1";

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Before
    public void setup() {
        tnsApiClient = new TnsApiClient("http://localhost:" + wireMockRule1.port(), "/tns/api/replier", "replyts", "replyts", 1, 400, 50, 300);
        postRequest = post(urlEqualTo("/tns/api/replier/ad/" + adId + "/increment-reply-count"));
        statusOkResponse = aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"OK\"}");
        delayedResponse = aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"OK\"}")
                .withFixedDelay(2000);
    }

    @Test
    public void incrementReplyCount_noException() throws Exception{
        stubFor(postRequest.willReturn(statusOkResponse));
        tnsApiClient.incrementReplyCount(adId);
    }

    @Test
    public void incrementReplyCount_retryOnce_firstCallTimedOut() throws Exception{
        stubFor(postRequest
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("first try")
            .willReturn(delayedResponse));
        stubFor(postRequest
                .inScenario("retry")
                .whenScenarioStateIs("first try")
                .willSetStateTo("second try")
                .willReturn(statusOkResponse));
        tnsApiClient.incrementReplyCount(adId);
        verify(2, postRequestedFor(urlEqualTo("/tns/api/replier/ad/1/increment-reply-count")));
    }

    @Test
    public void incrementReplyCount_max2Retries_timeOutExceptionThrown_allCallTimedOut() throws Exception {
        stubFor(postRequest.willReturn(delayedResponse));

        try {
            tnsApiClient.incrementReplyCount(adId);
        } catch (Exception e) {
            assertEquals(HystrixRuntimeException.class, e.getClass());
            verify(2, postRequestedFor(urlEqualTo("/tns/api/replier/ad/1/increment-reply-count")));
        }
    }

    @Test
    public void jsonObject_convertedToMap() {
        // This validates that we correct for the missing "/api" in the API base URL without having to add it to each call to TnsApiClient#getJsonAsMap()
        stubFor(get(urlEqualTo("/api/replier/email/user@example.com/is-new")).willReturn(aResponse().withStatus(200).withBody("{\"thing\":true}")));

        final Map map = tnsApiClient.getJsonAsMap("/replier/email/user@example.com/is-new");

        Assert.assertTrue(map.containsKey("thing"));
        Assert.assertThat(map.get("thing"), Is.is(true));
    }

    @After
    public void clearRequests() {
        resetAllRequests();
    }
}
