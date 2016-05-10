package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * User: acharton
 * Date: 12/17/12
 */

@RunWith(MockitoJUnitRunner.class)
public class Ip2CountryFilterTest {


    private Ip2CountryFilter ip2CountryFilter;

    @Mock
    private Ip2CountryRules countryRules;
    @Mock
    private Ip2CountryResolver resolver;
    @Mock
    private IpAddressExtractor extractor;
    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Before
    public void setUp() throws Exception {
        ip2CountryFilter = new Ip2CountryFilter(countryRules, resolver, extractor);

        when(extractor.retrieveIpAddress(messageProcessingContext)).thenReturn(Optional.of("192.168.0.1"));
        when(resolver.resolve("192.168.0.1")).thenReturn(Optional.of("RU"));
        when(countryRules.getScoreForCountry("RU")).thenReturn(400);
    }

    @Test
    public void filterFiresOnResolvableCountry() throws Exception {
        List<FilterFeedback> feedbacks = ip2CountryFilter.filter(messageProcessingContext);
        assertEquals(1, feedbacks.size());

        FilterFeedback filterFeedback = feedbacks.get(0);
        assertThat(filterFeedback.getDescription()).isEqualTo("Mail from country: RU");
        assertThat(filterFeedback.getResultState()).isEqualTo(FilterResultState.OK);
        assertThat(filterFeedback.getScore()).isEqualTo(400);
        assertThat(filterFeedback.getUiHint()).isEqualTo("RU");
    }

    @Test
    public void ignoreByIpResolver() throws Exception {
        when(resolver.resolve(anyString())).thenReturn(Optional.<String>absent());

        assertTrue(ip2CountryFilter.filter(messageProcessingContext).isEmpty());
    }

    @Test
    public void ignoreWhenIpNotExtractable() throws Exception {
        when(extractor.retrieveIpAddress(messageProcessingContext)).thenReturn(Optional.<String>absent());

        assertTrue(ip2CountryFilter.filter(messageProcessingContext).isEmpty());
    }

    @Test
    public void noFeedbackIfCountryHasScore0() throws Exception {
        when(countryRules.getScoreForCountry("RU")).thenReturn(0);

        assertTrue(ip2CountryFilter.filter(messageProcessingContext).isEmpty());
    }
}
