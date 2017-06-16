package ca.kijiji.replyts.countryblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static ca.kijiji.replyts.BoxHeaders.SENDER_IP_ADDRESS;
import static ca.kijiji.replyts.countryblockedfilter.CountryBlockedFilter.IS_COUNTRY_BLOCKED_KEY;
import static com.ecg.replyts.core.api.model.conversation.FilterResultState.DROPPED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class CountryBlockedFilterTest {
    private static final int SCORE = 100;

    @Tested
    private CountryBlockedFilter countryBlockedFilter;

    @Injectable
    private TnsApiClient tnsApiClient;

    @Mocked
    private MessageProcessingContext mpc;

    @Mocked
    private Mail mail;

    @Before
    public void setUp() throws Exception {
        countryBlockedFilter = new CountryBlockedFilter(SCORE, tnsApiClient);
    }

    @Test
    public void ipPresent_countryBlocked() throws Exception {
        final String ipAddress = "1.2.3.4";

        new Expectations() {{
            mpc.getMail();
            result = mail;

            mail.getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());
            result = ipAddress;

            tnsApiClient.getJsonAsMap("/replier/ip-address/1.2.3.4/is-country-blocked");
            result = ImmutableMap.of(IS_COUNTRY_BLOCKED_KEY, Boolean.TRUE);
        }};

        List<FilterFeedback> feedbacks = countryBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(1));
        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getUiHint(), is("country is blocked"));
        assertThat(feedback.getDescription(), is("IP country is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
    }

    @Test
    public void ipBlank_countryNotBlocked_gridNotContacted() throws Exception {
        final String ipAddress = "";

        new Expectations() {{
            mpc.getMail();
            result = mail;

            mail.getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());
            result = ipAddress;
        }};

        List<FilterFeedback> feedbacks = countryBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(0));
        new FullVerifications(tnsApiClient) {};
    }

    @Test
    public void noIp_countryNotBlocked_gridNotContacted() throws Exception {
        final String ipAddress = "";

        new Expectations() {{
            mpc.getMail();
            result = mail;

            mail.getUniqueHeader(SENDER_IP_ADDRESS.getHeaderName());
            result = ipAddress;
        }};

        List<FilterFeedback> feedbacks = countryBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(0));
        new FullVerifications(tnsApiClient) {};
    }
}
