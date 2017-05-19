package com.gumtree.comaas.filter.blacklist;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableMap;
import com.gumtree.comaas.filter.category.GumtreeCategoryBreadcrumbFilter;
import com.gumtree.filters.comaas.config.CategoryFilterConfig;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GumtreeCategoryBreadcrumbFilterTest {
    @InjectMocks
    private GumtreeCategoryBreadcrumbFilter filter;

    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Mock
    private Timer timer;

    @Test
    public void testCategoryWhenActive() {
        filter.withFilterConfig(new CategoryFilterConfig.Builder(State.ENABLED, 1, Result.STOP_FILTERING, null).build());
        Conversation conversation = mock(Conversation.class);
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("CATEGORYID", "1"));
        when(messageProcessingContext.getConversation()).thenReturn(conversation);

        filter.filter(messageProcessingContext);
    }
}