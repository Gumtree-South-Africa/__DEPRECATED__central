package com.ecg.gumtree.comaas.common.filter;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GumshieldClientTest {

    private static final String RESPONSE = "{\"id\":11312992,\"type\":\"BLACK\",\"attribute\":\"EMAIL\"," +
            "\"value\":\"naijaofe50@outlook.com\",\"created_date\":\"2019-01-16T04:38:42Z\"," +
            "\"notes\":\"Blacklist Related Ad:1326813081. Added by Deleting Message:baabf947-190c-11e9-a944-021547527eba\"," +
            "\"last_modified_date\":\"2019-01-16T04:38:42Z\"}";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Options.DYNAMIC_PORT);

    private GumshieldClient gumshieldClient;

    @Before
    public void setup() {
        gumshieldClient = new GumshieldClient(String.format("http://localhost:%d/api", wireMockRule.port()));
    }

    @Test
    public void returnsFalseForGumshieldAPI200Response() {
                stubFor(get(urlMatching(".*/api/checklists/BLACK/EMAIL/naijaofe50@outlook.com")).
                willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json;charset=utf8").
                        withBody(RESPONSE.getBytes())));

        assertTrue(gumshieldClient.checkContainsRecord(ApiChecklistType.BLACK, ApiChecklistAttribute.EMAIL, "naijaofe50@outlook.com"));
    }

    @Test
    public void returnsFalseForGumshieldAPI404Response() {
        stubFor(get(urlMatching(".*/api/checklists/BLACK/EMAIL/naijaofe50@outlook.com")).
                willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/json;charset=utf8")));

        assertFalse(gumshieldClient.checkContainsRecord(ApiChecklistType.BLACK, ApiChecklistAttribute.EMAIL, "naijaofe50@outlook.com"));
    }

    @Test
    public void returnsFalseForGumshieldAPI500Response() {
        stubFor(get(urlMatching(".*/api/checklists/BLACK/EMAIL/naijaofe50@outlook.com")).
                willReturn(aResponse().withStatus(500).withHeader("Content-Type", "application/json;charset=utf8")));

        assertFalse(gumshieldClient.checkContainsRecord(ApiChecklistType.BLACK, ApiChecklistAttribute.EMAIL, "naijaofe50@outlook.com"));
    }
}