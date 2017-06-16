package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class NewReplierFilterTest {
    private static final String FROM_EMAIL = "from@kijiji.ca";
    private static final String GRID_API_END_POINT = "http://localhost/api";
    private static final int SCORE = 100;

    @ClassRule
    public static WireMockClassRule wireMockRule1 = new WireMockClassRule(0);

    @Mocked
    private MessageProcessingContext mpc;

    @Mocked
    private Mail mail;

    private JsonNode jsonNode;

    private NewReplierFilter newReplierFilter;

    @Before
    public void setUp() throws Exception {
        jsonNode = new ObjectMapper().readTree("{}");

        new NonStrictExpectations() {{
            mpc.getMail();
            result = mail;

            mail.getFrom();
            result = FROM_EMAIL;
        }};

        final TnsApiClient tnsApiClient = new TnsApiClient("http://localhost:" + wireMockRule1.port(), "/api", "replyts", "replyts", 1, 400, 50, 300);
        newReplierFilter = new NewReplierFilter(100, new ca.kijiji.replyts.Activation(jsonNode), tnsApiClient);
    }

    @Test
    public void whenReplierIsNew_thenScoreInFeedback() throws Exception {
        stubFor(get(urlEqualTo("/api/replier/email/" + FROM_EMAIL + "/is-new")).willReturn(WireMock.aResponse().withStatus(200).withBody("{\"is-new\":true}")));

        List<FilterFeedback> feedbacks = newReplierFilter.doFilter(mpc);
        assertEquals(1, feedbacks.size());
        final FilterFeedback feedback = feedbacks.get(0);
        assertEquals("email is new", feedback.getUiHint());
        assertEquals("Replier email is new", feedback.getDescription());
        assertEquals(FilterResultState.OK, feedback.getResultState());
        assertEquals(SCORE, feedback.getScore().intValue());
    }

    @Test
    public void whenReplierIsOld_thenEmptyFeedback() throws Exception {
        stubFor(get(urlEqualTo("/api/replier/email/" + FROM_EMAIL + "/is-new")).willReturn(WireMock.aResponse().withStatus(200).withBody("{\"is-new\":false}")));

        List<FilterFeedback> feedbacks = newReplierFilter.doFilter(mpc);
        assertEquals(0, feedbacks.size());
    }
}
