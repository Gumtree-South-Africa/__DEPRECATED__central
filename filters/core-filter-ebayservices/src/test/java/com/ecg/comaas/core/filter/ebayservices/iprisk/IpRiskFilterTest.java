package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ebay.marketplace.security.v1.services.IPBadLevel;
import com.ebay.marketplace.security.v1.services.IPRatingInfo;
import com.ecg.comaas.core.filter.ebayservices.IpAddressExtractor;
import com.ecg.comaas.core.filter.ebayservices.iprisk.IpRiskFilter;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IpRiskFilterTest {

    private IpRiskFilter ipRiskFilter;

    @Mock
    private IpAddressExtractor iae;
    @Mock
    private IpRatingService irs;
    @Mock
    private MessageProcessingContext mpc;
    @Mock
    private IPRatingInfo ratingInfo;

    @Before
    public void setUp() throws Exception {
        when(iae.retrieveIpAddress(mpc)).thenReturn(Optional.of("192.168.2.2"));
        when(irs.getIpRating(anyString())).thenReturn(ratingInfo);

        ipRiskFilter = new IpRiskFilter("testFilter", ImmutableMap.of("BAD", 100, "GOOD", 0), irs, iae);
    }

    @Test
    public void rateBad() throws Exception {
        when(ratingInfo.getIpBadLevel()).thenReturn(IPBadLevel.BAD);

        List<FilterFeedback> feedbacks = feedback();
        assertEquals(1, feedbacks.size());

        FilterFeedback filterFeedback = feedbacks.get(0);
        assertThat(filterFeedback.getDescription()).isEqualTo("IP is rated as: BAD");
        assertThat(filterFeedback.getResultState()).isEqualTo(FilterResultState.OK);
        assertThat(filterFeedback.getScore()).isEqualTo(100);
        assertThat(filterFeedback.getUiHint()).isEqualTo("192.168.2.2");
    }

    @Test
    public void ingoreNeutralRating() throws Exception {
        when(ratingInfo.getIpBadLevel()).thenReturn(IPBadLevel.GOOD);

        assertTrue(feedback().isEmpty());
    }

    @Test
    public void ignoreIfNoIp() throws Exception {
        when(iae.retrieveIpAddress(mpc)).thenReturn(Optional.empty());

        assertTrue(feedback().isEmpty());
    }

    @Test
    public void ignoreEbayServiceExceptions() throws Exception {
        when(irs.getIpRating(anyString())).thenThrow(new ServiceException("Test"));

        assertTrue(feedback().isEmpty());

    }

    private List<FilterFeedback> feedback() {
        return  ipRiskFilter.filter(mpc);
    }


}
