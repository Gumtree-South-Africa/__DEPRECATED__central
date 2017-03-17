package com.gumtree.replyts2.notifier;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.gumtree.test.utils.Fixtures;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatsApiNotifierTest {

    private static final String STATS_URL = "http://localhost:9090/stats-api:123";

    private StatsApiNotifier statsApiNotifier;

    @Mock
    private ProcessingFeedback processingFeedback;

    @Mock
    private AsyncHttpClient.BoundRequestBuilder builder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AsyncHttpClient asyncHttpClient;

    @Mock
    private Timer statsTimer;

    @Before
    public void setup() {
        statsApiNotifier = new StatsApiNotifier(STATS_URL, asyncHttpClient);
    }

    @Test
    public void testSendAsyncNotification() throws Exception {
        String postUrl = String.format("%s/advert-stats/%s/counts/reply", STATS_URL, "123");
        Map<String, List<String>> params = ImmutableMap.of("from", ImmutableList.of("buyer@test.com"));
        when(asyncHttpClient.preparePost(postUrl)).thenReturn(builder);
        when(builder.setHeader("Content-Type", "application/x-www-form-urlencoded")).thenReturn(builder);
        when(builder.setFormParams(params)).thenReturn(builder);
        when(builder.execute(Matchers.any(AsyncCompletionHandler.class))).thenReturn(null);

        Message message = Fixtures.buildMessage(MessageDirection.BUYER_TO_SELLER, processingFeedback);
        Conversation conversation = Fixtures.buildConversation(ImmutableList.of(message));

        statsApiNotifier.sendAsyncNotification(conversation.getBuyerId(), conversation.getAdId(), statsTimer);

        verify(asyncHttpClient).preparePost(eq(postUrl));
        verify(builder).setHeader("Content-Type", "application/x-www-form-urlencoded");
        verify(builder).setFormParams(ImmutableMap.of("from", ImmutableList.of(conversation.getBuyerId())));
    }
}