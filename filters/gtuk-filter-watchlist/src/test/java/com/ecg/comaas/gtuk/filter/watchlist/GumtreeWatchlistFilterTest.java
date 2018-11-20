package com.ecg.comaas.gtuk.filter.watchlist;

import com.ecg.gumtree.comaas.common.filter.GumshieldClient;
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
import com.gumtree.common.util.http.NotFoundHttpStatusException;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistEntry;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeWatchlistFilterTest.TestContext.class)
public class GumtreeWatchlistFilterTest {
    private static final String messageId = "123";

    @Autowired
    private GumtreeWatchlistFilter gumtreeWatchlistFilter;

    @Autowired
    private GumshieldClient gumshieldClient;

    @Test
    public void testExemptedCategoriesReturnsEmptyList() {
        Mail mail = mock(Mail.class);
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail);
        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", ImmutableSet.of(1234L));

        List<FilterFeedback> feedbacks = gumtreeWatchlistFilter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void testWatchlistedSender() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("badguy@hotmail.com");
        MessageProcessingContext messageProcessingContext = getMessageProcessingContext(mail);

        List<FilterFeedback> feedbacks = gumtreeWatchlistFilter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(1);
        assertThat(feedbacks.get(0).getResultState()).isEqualTo(FilterResultState.HELD);
    }

    @Test
    public void testGoodSender() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("goodguy@hotmail.com");
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL),
                eq("goodguy@hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.HOST),
                eq("1.1.1.1"))).thenThrow(new NotFoundHttpStatusException());

        List<FilterFeedback> feedbacks = gumtreeWatchlistFilter.filter(getMessageProcessingContext(mail));
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void testWatchlistedSenderEmailDomain() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("goodguy@hotmail.com");
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL),
                eq("goodguy@hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenReturn(new ApiChecklistEntry());

        List<FilterFeedback> feedbacks = gumtreeWatchlistFilter.filter(getMessageProcessingContext(mail));
        assertThat(feedbacks.size()).isEqualTo(1);
        assertThat(feedbacks.get(0).getUiHint()).isEqualTo("hotmail.com");
    }

    @Test
    public void testWatchlistedSenderIpAddress() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("goodguy@hotmail.com");
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL),
                eq("goodguy@hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(gumshieldClient.checkByValue(eq(ApiChecklistType.WATCH), eq(ApiChecklistAttribute.HOST),
                eq("1.1.1.1"))).thenReturn(new ApiChecklistEntry());

        List<FilterFeedback> feedbacks = gumtreeWatchlistFilter.filter(getMessageProcessingContext(mail));
        assertThat(feedbacks.size()).isEqualTo(1);
        assertThat(feedbacks.get(0).getUiHint()).isEqualTo("1.1.1.1");
    }

    private MessageProcessingContext getMessageProcessingContext(Mail mail) {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, messageId,
                new ProcessingTimeGuard(1L));

        messageProcessingContext.setConversation(conversation);
        return messageProcessingContext;
    }

    @Configuration
    static class TestContext {
        @MockBean
        GumshieldClient checklistApi;

        @Bean
        public WatchlistFilterConfig filterConfig() throws Exception {
            return new WatchlistFilterConfig.Builder(State.ENABLED, 1, Result.HOLD, false,
                    10, 10, "")
                    .withStub(true)
                    .withExemptedCategories(ImmutableList.of(1234L, 4321L))
                    .withStubWatchlistedUsers(ImmutableList.of("badguy@hotmail.com", "badguy@yahoo.com"))
                    .build();
        }

        @Bean
        public GumtreeWatchlistFilter filter(WatchlistFilterConfig filterConfig) {
            return new GumtreeWatchlistFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig)
                    .withChecklistApi(checklistApi);
        }
    }
}
