package com.ecg.comaas.gtuk.filter.geoip;

import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeGeoIpFilterHeldTest.TestContext.class)
public class GumtreeGeoIpFilterHeldTest {
    private static final String RESOLVABLE_IP = "1.2.3.4";
    private static final String UNCONF_CTRY_IP = "127.1.2.3";
    private static final String BANNED_COUNTRY_IP = "255.2.3.4";

    private static final String messageId = "123";

    private static List<Long> exemptedCategories = ImmutableList.of(1234L, 4321L);

    @Autowired
    private GumtreeGeoIpFilter filter;

    @Test
    public void testKnownButUnconfiguredCountry() {
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext("ZA", UNCONF_CTRY_IP);

        List<FilterFeedback> feedbackList = filter.filter(messageProcessingContext);
        assertEquals(0, feedbackList.size());
    }

    @Test
    public void testDoCheckForHeldCountry() {
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext("DE", RESOLVABLE_IP);

        List<FilterFeedback> feedbackList = filter.filter(messageProcessingContext);
        assertEquals(1, feedbackList.size());

        assertEquals(feedbackList.get(0).getResultState(), FilterResultState.HELD);
        assertEquals(feedbackList.get(0).getUiHint(), RESOLVABLE_IP);
    }

    @Test
    public void testDoCheckForBannedCountry() {
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext("NG", BANNED_COUNTRY_IP);

        List<FilterFeedback> feedbackList = filter.filter(messageProcessingContext);
        assertEquals(0, feedbackList.size());
    }

    private MessageProcessingContext getMessageProcessingContext(String country, String ip) {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        when(message.getCaseInsensitiveHeaders()).thenReturn(ImmutableMap.of(GumtreeCustomHeaders.BUYER_IP.getHeaderValue(), ip));
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mock(Mail.class), messageId,
                new ProcessingTimeGuard(1L));
        messageProcessingContext.setConversation(conversation);
        messageProcessingContext.getFilterContext().put("country", country);
        return messageProcessingContext;
    }

    @Configuration
    static class TestContext {
        @Bean
        public GeoIpFilterConfig filterConfig() throws Exception {
            return new GeoIpFilterConfig.Builder(State.ENABLED, 1, Result.HOLD)
                    .withExemptedCategories(exemptedCategories)
                    .withCountrySet(ImmutableList.of("DE"))
                    .build();
        }

        @Bean
        public GumtreeGeoIpFilter filter(GeoIpFilterConfig filterConfig) {
            return new GumtreeGeoIpFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig);
        }
    }
}