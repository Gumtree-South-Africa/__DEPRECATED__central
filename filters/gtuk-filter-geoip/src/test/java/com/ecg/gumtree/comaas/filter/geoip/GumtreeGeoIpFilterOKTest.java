package com.ecg.gumtree.comaas.filter.geoip;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.collect.ImmutableList;
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

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeGeoIpFilterOKTest.TestContext.class)
public class GumtreeGeoIpFilterOKTest {
    private static final String messageId = "123";

    private static List<Long> exemptedCategories = ImmutableList.of(1234L, 4321L);

    @Autowired
    private GumtreeGeoIpFilter filter;

    @Test
    public void testFilterNotAppliedInExemptCategory() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail, message);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    private MessageProcessingContext getMessageProcessingContext(Mail mail, Message message) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, messageId,
                new ProcessingTimeGuard(1L));
        messageProcessingContext.setConversation(conversation);
        messageProcessingContext.getFilterContext().put("country", "NG");
        return messageProcessingContext;
    }

    @Configuration
    static class TestContext {
        @Bean
        public GeoIpFilterConfig filterConfig() throws Exception {
            return new GeoIpFilterConfig.Builder(State.ENABLED, 1, Result.STOP_FILTERING)
                    .withExemptedCategories(exemptedCategories)
                    .withCountrySet(ImmutableList.of("UK"))
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