package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Expectations;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class NewReplierFilterTest {

    private static final String FROM_EMAIL = "from@kijiji.ca";
    private static final String GRID_API_END_POINT = "http://legrid/api";
    private static final int SCORE = 100;

    @Mocked
    private MessageProcessingContext mpc;

    @Mocked
    private Mail mail;

    @Mocked
    private ClientBuilder clientBuilder;

    @Mocked
    private Client client;

    @Mocked
    private WebTarget webTarget;

    @Mocked
    private Invocation.Builder rsBuilder;

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

            ClientBuilder.newClient((Configuration) any);
            result = client;

            client.target(GRID_API_END_POINT);
            result = webTarget;

            webTarget.path(anyString);
            result = webTarget;

            webTarget.request(MediaType.APPLICATION_JSON);
            result = rsBuilder;
        }};

        newReplierFilter = new NewReplierFilter(100, new LeGridClient(GRID_API_END_POINT, "user", "password"),
                new ca.kijiji.replyts.Activation(jsonNode));
    }

    @Test
    public void whenReplierIsNew_thenScoreInFeedback() throws Exception {
        final HashMap<String, Object> gridResponseMap = new HashMap<String, Object>();
        gridResponseMap.put(NewReplierFilter.IS_NEW_KEY, Boolean.TRUE);

        new Expectations() {{
            rsBuilder.get(Map.class);
            result = gridResponseMap;
        }};

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
        final HashMap<String, Object> gridResponseMap = new HashMap<String, Object>();
        gridResponseMap.put(NewReplierFilter.IS_NEW_KEY, Boolean.FALSE);

        new Expectations() {{
            rsBuilder.get(Map.class);
            result = gridResponseMap;
        }};

        List<FilterFeedback> feedbacks = newReplierFilter.doFilter(mpc);
        assertEquals(0, feedbacks.size());
    }
}
