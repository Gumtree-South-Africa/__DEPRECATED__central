package com.ecg.messagecenter.listeners;

import com.ecg.messagecenter.capi.AdInfoLookup;
import com.ecg.messagecenter.capi.UserInfoLookup;
import com.ecg.messagecenter.persistence.PostBoxInitializer;
import com.ecg.messagecenter.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxUpdateListenerTest {
    private PostBoxUpdateListener listener;

    @Mock
    private PostBoxInitializer postBoxInitializer;
    @Mock
    private PushService pushService;
    @Mock
    private PushService sendPushService;
    @Mock
    private AdInfoLookup adInfoLookup;
    @Mock
    private UserInfoLookup userInfoLookup;

    private ImmutableConversation.Builder convoBuilder;
    private ImmutableMessage.Builder msgBuilder;

    @Before
    public void setUp() throws Exception {
        listener = new PostBoxUpdateListener(postBoxInitializer, false, "capi", 80, "username", "password", 1000, 1000, 1000, 1, 0, 0, null, sendPushService);

        convoBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId("cid")
                .withState(ConversationState.DEAD_ON_ARRIVAL);
        msgBuilder = ImmutableMessage.Builder
                .aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.ORPHANED)
                .withReceivedAt(now(UTC))
                .withLastModifiedAt(now(UTC))
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.UNCHECKED)
                .withHeaders(ImmutableMap.of())
                .withTextParts(ImmutableList.of(""))
                .withProcessingFeedback(ImmutableList.of())
                .withLastEditor(Optional.absent());
    }

    @Test
    public void validMsgAndConvo_processed() throws Exception {
        Conversation conversation = convoBuilder
                .withState(ConversationState.ACTIVE)
                .withSeller("seller@example.com", "sellersecret")
                .withBuyer("buyer@example.com", "buyersecret")
                .build();
        Message message = msgBuilder.withState(MessageState.SENT).build();

        listener.messageProcessed(conversation, message);

        verify(postBoxInitializer).moveConversationToPostBox(
                eq(conversation.getSellerId()),
                eq(conversation),
                eq(true),
                any());
        verify(postBoxInitializer).moveConversationToPostBox(
                eq(conversation.getBuyerId()),
                eq(conversation),
                eq(false),
                any());
    }

    @Test
    public void convoDeadOnArrival_notProcessed() throws Exception {
        Conversation conversation = convoBuilder.build();
        Message message = msgBuilder.build();

        listener.messageProcessed(conversation, message);

        verifyNoMoreInteractions(postBoxInitializer, pushService, adInfoLookup, userInfoLookup);
    }

    @Test
    public void noSellerId_notProcessed() throws Exception {
        Conversation conversation = convoBuilder
                .withState(ConversationState.ACTIVE)
                .withBuyer("buyer@example.com", "buyersecret")
                .build();
        Message message = msgBuilder.build();

        listener.messageProcessed(conversation, message);

        verifyNoMoreInteractions(postBoxInitializer, pushService, adInfoLookup, userInfoLookup);
    }

    @Test
    public void noBuyerId_notProcessed() throws Exception {
        Conversation conversation = convoBuilder
                .withState(ConversationState.ACTIVE)
                .withSeller("seller@example.com", "sellersecret")
                .build();
        Message message = msgBuilder.build();

        listener.messageProcessed(conversation, message);

        verifyNoMoreInteractions(postBoxInitializer, pushService, adInfoLookup, userInfoLookup);
    }

    @Test
    public void conversationNotAnonymized_notProcessed() throws Exception {
        Conversation conversation = convoBuilder
                .withState(ConversationState.ACTIVE)
                .withSeller("seller@example.com", "sellersecret")
                .withBuyer("buyer@example.com", "buyersecret")
                .withCustomValues(ImmutableMap.of("anonymize", "false"))
                .build();
        Message message = msgBuilder.withState(MessageState.SENT).build();

        listener.messageProcessed(conversation, message);
        verifyNoMoreInteractions(postBoxInitializer, pushService, adInfoLookup, userInfoLookup);
    }

    @Test(expected = RuntimeException.class)
    public void exceptionDuringProcessing_propagate() throws Exception {
        Conversation conversation = convoBuilder
                .withState(ConversationState.ACTIVE)
                .withSeller("seller@example.com", "sellersecret")
                .withBuyer("buyer@example.com", "buyersecret")
                .build();
        Message message = msgBuilder.withState(MessageState.SENT).build();

        doThrow(new RuntimeException()).when(postBoxInitializer).moveConversationToPostBox(
                eq(conversation.getSellerId()),
                eq(conversation),
                eq(true),
                any());

        listener.messageProcessed(conversation, message);
    }
}
