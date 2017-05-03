package com.gumtree.comaas.filter.geoiplookup;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.GeoIpLookupConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gumtree.MockFactory.mockConversation;
import static com.gumtree.MockFactory.mockMessage;
import static com.gumtree.filters.comaas.config.Result.HOLD;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeGeoIpLookupFilterTest.TestContext.class)
public class GumtreeGeoIpLookupFilterTest {
    private static final String GERMAN_IP = "1.2.3.4";
    private static final String UNKNOWN_IP = "4.3.2.1";
    private static final String SAFFER_IP = "127.1.2.3";
    private static final String BANNED_COUNTRY_IP = "255.2.3.4";

    private static final String messageId = "123";

    private Set<Integer> breadCrumb = ImmutableSet.of(987);

    @Autowired
    private GumtreeGeoIpLookupFilter filter;

    @Test
    public void testFilterNotAppliedInS2BDirection() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        Map<String, String> headers = ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), UNKNOWN_IP);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    @Test
    public void testNoIp() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        Map<String, String> headers = new HashMap<>();
        headers.put(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), null);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    @Test
    public void testUnknownCountry() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        Map<String, String> headers = ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), UNKNOWN_IP);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    @Test
    public void testForSouthAfricanIp() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        Map<String, String> headers = ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), SAFFER_IP);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(1, feedbacks.size());
        assertEquals("{\"filterVersion\":\"\",\"filterName\":\"com.gumtree.comaas.filter.geoiplookup.GumtreeGeoIpLookupFilter\",\"description\":\"ZA\"}", feedbacks.get(0).getDescription());
        assertEquals(FilterResultState.HELD, feedbacks.get(0).getResultState());
    }

    @Test
    public void testForGermanIp() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        Map<String, String> headers = ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), GERMAN_IP);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(1, feedbacks.size());
        assertEquals(FilterResultState.HELD, feedbacks.get(0).getResultState());
        assertEquals("{\"filterVersion\":\"\",\"filterName\":\"com.gumtree.comaas.filter.geoiplookup.GumtreeGeoIpLookupFilter\",\"description\":\"DE\"}", feedbacks.get(0).getDescription());
    }

    @Test
    public void testForNigerianIp() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        Map<String, String> headers = ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), BANNED_COUNTRY_IP);
        when(message.getHeaders()).thenReturn(headers);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(1, feedbacks.size());
        assertEquals(FilterResultState.HELD, feedbacks.get(0).getResultState());
        assertEquals("{\"filterVersion\":\"\",\"filterName\":\"com.gumtree.comaas.filter.geoiplookup.GumtreeGeoIpLookupFilter\",\"description\":\"NG\"}", feedbacks.get(0).getDescription());
    }

    private MessageProcessingContext getMessageProcessingContext(Mail mail, Message message) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, messageId, new ProcessingTimeGuard(1L));

        messageProcessingContext.setConversation(conversation);
        return messageProcessingContext;
    }

    @Configuration
    static class TestContext {
        @Bean
        public GeoIpLookupConfig filterConfig() throws Exception {
            return new GeoIpLookupConfig.Builder(State.ENABLED, 1, HOLD)
                    .withStub(true)
                    .withAppName("Gumtree")
                    .withIafToken(null)
                    .withProxyUrl("")
                    .withStubGeoIpCountry(ImmutableMap.of(GERMAN_IP, "DE", BANNED_COUNTRY_IP, "NG", SAFFER_IP, "ZA"))
                    .withWebServiceUrl("")
                    .build();
        }

        @Bean
        public GeoIpService geoIpService() {
            GeoIpService mock = mock(GeoIpService.class);
            when(mock.getCountryCode(SAFFER_IP)).thenReturn("ZA");
            when(mock.getCountryCode(GERMAN_IP)).thenReturn("DE");
            when(mock.getCountryCode(BANNED_COUNTRY_IP)).thenReturn("NG");
            return mock;
        }

        @Bean
        public GumtreeGeoIpLookupFilter filter(GeoIpLookupConfig filterConfig) {
            return new GumtreeGeoIpLookupFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withGeoIPService(geoIpService())
                    .withFilterConfig(filterConfig);
        }
    }
}
