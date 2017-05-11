package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.gumtree.common.util.time.Clock;
import com.gumtree.common.util.time.StoppedClock;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class VolumeFilterServiceHelperTest {
    private final DateTime TEST_DATE_TIME = new DateTime();

    private VolumeFilterServiceHelper helper;
    private Clock clock;

    @Before
    public void setup() {
        clock = new StoppedClock(TEST_DATE_TIME);
        helper = new VolumeFilterServiceHelper(clock);
    }

    @Test
    public void testCreateSearchParametersWithEmail() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.EMAIL;
        String value = "bacon@gumtree.com";
        int seconds = 42;

        SearchMessagePayload searchParameters = helper.createSearchParameters(field, value, seconds);

        assertEquals(value, searchParameters.getUserEmail());
        assertEquals(SearchMessagePayload.ConcernedUserRole.SENDER, searchParameters.getUserRole());
        assertEquals(calculateExpectedStartDate(seconds), searchParameters.getFromDate());

    }

    @Test
    public void testCreateSearchParametersWithCookie() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.COOKIE;
        String value = "ABCDEFG";
        int seconds = 1046565;

        SearchMessagePayload parameters = helper.createSearchParameters(field, value, seconds);

        assertEquals(1, parameters.getConversationCustomValues().size());
        assertEquals(value, parameters.getConversationCustomValues().get(ElasticsearchCustomHeaderKeys.BUYER_COOKIE.getCustomHeaderKey()));
        assertEquals(calculateExpectedStartDate(seconds), parameters.getFromDate());

    }

    @Test
    public void testCreateSearchParametersWithIpAddress() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.IP_ADDRESS;
        String value = "1.2.3.4";
        int seconds = 6;

        SearchMessagePayload parameters = helper.createSearchParameters(field, value, seconds);

        assertEquals(1, parameters.getConversationCustomValues().size());
        assertEquals(value, parameters.getConversationCustomValues().get(ElasticsearchCustomHeaderKeys.BUYER_IP.getCustomHeaderKey()));
        assertEquals(calculateExpectedStartDate(seconds), parameters.getFromDate());

    }

    @Test
    public void testCreateWhitelistSearchParametersWithEmail() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.EMAIL;
        String value = "bacon@gumtree.com";
        int whitelistSeconds = 42;

        SearchMessagePayload parameters = helper.createWhitelistSearchParameters(field, value, whitelistSeconds);

        assertEquals(value, parameters.getUserEmail());
        assertEquals(SearchMessagePayload.ConcernedUserRole.SENDER, parameters.getUserRole());
        assertEquals(calculateExpectedStartDate(whitelistSeconds), parameters.getFromDate());
        assertEquals(ModerationResultState.GOOD, parameters.getHumanResultState());
        assertEquals(1, parameters.getCount());
    }

    @Test
    public void testCreateWhitelistSearchParametersWithCookie() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.COOKIE;
        String value = "ABCDEFG";
        int whitelistSeconds = 42;

        SearchMessagePayload parameters = helper.createWhitelistSearchParameters(field, value, whitelistSeconds);

        assertEquals(1, parameters.getConversationCustomValues().size());
        assertEquals(value, parameters.getConversationCustomValues().get(ElasticsearchCustomHeaderKeys.BUYER_COOKIE.getCustomHeaderKey()));
        assertEquals(calculateExpectedStartDate(whitelistSeconds), parameters.getFromDate());
        assertEquals(ModerationResultState.GOOD, parameters.getHumanResultState());
        assertEquals(1, parameters.getCount());
    }

    @Test
    public void testCreateWhitelistSearchParametersWithIpAddress() throws Exception {
        VelocityFilterConfig.FilterField field = VelocityFilterConfig.FilterField.IP_ADDRESS;
        String value = "1.2.3.4";
        int whitelistSeconds = 42;

        SearchMessagePayload parameters = helper.createWhitelistSearchParameters(field, value, whitelistSeconds);

        assertEquals(1, parameters.getConversationCustomValues().size());
        assertEquals(value, parameters.getConversationCustomValues().get(ElasticsearchCustomHeaderKeys.BUYER_IP.getCustomHeaderKey()));
        assertEquals(calculateExpectedStartDate(whitelistSeconds), parameters.getFromDate());
        assertEquals(ModerationResultState.GOOD, parameters.getHumanResultState());
        assertEquals(1, parameters.getCount());
    }

    private Date calculateExpectedStartDate(int seconds) {
        return clock.getDateTime().minusSeconds(seconds).toDate();
    }
}
