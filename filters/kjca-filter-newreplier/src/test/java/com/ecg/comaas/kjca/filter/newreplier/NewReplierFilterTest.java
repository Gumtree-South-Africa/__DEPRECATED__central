package com.ecg.comaas.kjca.filter.newreplier;

import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewReplierFilterTest {

    private static final String FROM_EMAIL = "from@kijiji.ca";
    private static final int SCORE = 100;

    private NewReplierFilter objectUnderTest;

    @Mock
    private TnsApiClient tnsApiClientMock;

    @Mock
    private MessageProcessingContext mpcMock;

    @Mock
    private Mail mailMock;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8081));

    @Before
    public void setUp() throws IOException {
        objectUnderTest = new NewReplierFilter(SCORE, getActivation(), tnsApiClientMock);

        when(mailMock.getFrom()).thenReturn(FROM_EMAIL);
        when(mpcMock.getMail()).thenReturn(Optional.of(mailMock));
    }

    @Test
    public void whenUserEmailIsNull_shouldReturnEmptyFeedback() {
        when(mailMock.getFrom()).thenReturn(null);

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenUserEmailIsEmpty_shouldReturnEmptyFeedback() {
        when(mailMock.getFrom()).thenReturn("");

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiResultIsEmpty_shouldReturnEmptyFeedback() {
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(Collections.emptyMap());

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiResultIsFalse_shouldReturnEmptyFeedback() {
        Map<String, Boolean> tnsApiResult = Collections.singletonMap(NewReplierFilter.IS_NEW_KEY, Boolean.FALSE);
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(tnsApiResult);

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenTnsApiResultIsTrue_shouldReturnFeedback() {
        Map<String, Boolean> tnsApiResult = Collections.singletonMap(NewReplierFilter.IS_NEW_KEY, Boolean.TRUE);
        when(tnsApiClientMock.getJsonAsMap(anyString())).thenReturn(tnsApiResult);

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).hasSize(1);
        assertFeedback(actualFeedback.get(0));
    }

    @Test
    public void testActualHttpResponse() throws IOException {
        TnsApiClient tnsApiClient = new TnsApiClient("http", "localhost", wireMockRule.port(), "/api",
                "replyts", "replyts", 1, 400, 50, 300);
        objectUnderTest = new NewReplierFilter(SCORE, getActivation(), tnsApiClient);
        stubFor(get(urlEqualTo("/api/replier/email/" + FROM_EMAIL + "/is-new")).willReturn(WireMock.aResponse().withStatus(200).withBody("{\"is-new\":true}")));

        List<FilterFeedback> actualFeedback = objectUnderTest.doFilter(mpcMock);

        assertThat(actualFeedback).hasSize(1);
        assertFeedback(actualFeedback.get(0));
    }

    private static void assertFeedback(FilterFeedback actual) {
        assertThat(actual.getUiHint()).isEqualToIgnoringCase(NewReplierFilter.UI_HINT);
        assertThat(actual.getDescription()).isEqualToIgnoringCase(NewReplierFilter.DESCRIPTION);
        assertThat(actual.getResultState()).isEqualTo(FilterResultState.OK);
        assertThat(actual.getScore()).isEqualTo(SCORE);
    }

    private static Activation getActivation() throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree("{}");
        return new Activation(jsonNode);
    }
}
