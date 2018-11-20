package com.ecg.comaas.gtuk.filter.knowngood;

import com.ecg.gumtree.comaas.common.filter.GumshieldClient;
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
import com.google.common.collect.ImmutableSet;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodResponse;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeKnownGoodFilterTest.TestContext.class)
public class GumtreeKnownGoodFilterTest {
    private static final String messageId = "123";

    private Set<Long> breadCrumb = ImmutableSet.of(987L);

    @Autowired
    private GumtreeKnownGoodFilter filter;

    @Test
    public void testKnownGoodNotFiredForExemptedCategory() {
        Set<Long> breadCrumbWithExemptedCategory = ImmutableSet.of(1234L);

        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mockMessage(MessageDirection.SELLER_TO_BUYER), breadCrumbWithExemptedCategory);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    @Test
    public void testKnownGoodBuyerFirstMessage() {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        message.getHeaders().put(GumtreeCustomHeaders.BUYER_ID.getHeaderValue(), "42");
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(message, null);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(feedbacks.size(), 1);

        assertEquals(FilterResultState.ACCEPT_AND_TERMINATE, feedbacks.get(0).getResultState());
        assertEquals("{\"filterVersion\":\"\",\"filterName\":\"com.ecg.comaas.gtuk.filter.knowngood.GumtreeKnownGoodFilter\",\"description\":\"Sender is known good\"}", feedbacks.get(0).getDescription());
    }

    @Test
    public void testKnownGoodBuyerFirstReply() {
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mockMessage(MessageDirection.SELLER_TO_BUYER), null);
        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(feedbacks.size(), 0);
    }

    @Test
    public void testKnownGoodSellerFirstReply() {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        message.getHeaders().put(GumtreeCustomHeaders.SELLER_ID.getHeaderValue(), "23");
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(message, null);

        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(1, feedbacks.size());

        assertEquals(FilterResultState.ACCEPT_AND_TERMINATE, feedbacks.get(0).getResultState());
        assertEquals("{\"filterVersion\":\"\",\"filterName\":\"com.ecg.comaas.gtuk.filter.knowngood.GumtreeKnownGoodFilter\",\"description\":\"Sender is known good\"}", feedbacks.get(0).getDescription());
    }

    @Test
    public void testKnownGoodSellerFirstMessage() {
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mockMessage(MessageDirection.BUYER_TO_SELLER), null);
        List<FilterFeedback> feedbacks = filter.filter(messageProcessingContext);
        assertEquals(feedbacks.size(), 0);
    }

    private MessageProcessingContext getMessageProcessingContext(Message message, Set<Long> categoryBreadCrumb) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mockMail(), messageId,
                new ProcessingTimeGuard(1L));
        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", categoryBreadCrumb);

        messageProcessingContext.setConversation(conversation);
        return messageProcessingContext;
    }

    private Mail mockMail() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("user@hotmail.com");
        return mail;
    }

    @Configuration
    static class TestContext {
        @Bean
        public KnownGoodFilterConfig filterConfig() throws Exception {
            return new KnownGoodFilterConfig.Builder(State.ENABLED, 1, Result.STOP_FILTERING, "", "")
                    .withExemptedCategories(ImmutableList.of(1234L, 4321L))
                    .build();
        }

        @Bean
        public GumtreeKnownGoodFilter filter(KnownGoodFilterConfig filterConfig) {
            GumshieldClient userApi = mock(GumshieldClient.class);
            when(userApi.knownGood(eq(42L))).thenReturn(new KnownGoodResponse(42L, KnownGoodStatus.GOOD));
            when(userApi.knownGood(eq(23L))).thenReturn(new KnownGoodResponse(23L, KnownGoodStatus.GOOD));
            return new GumtreeKnownGoodFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig)
                    .withUserApi(userApi);
        }
    }
}