package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxInitializerTest {
    public static final String SELLER_EMAIL = "seller@example.com";
    public static final String AD_ID = "adid";
    public static final String CONV_ID = "convId";
    public static final String BUYER_EMAIL = "buyer@example.com";
    public static final String BUYER_SECRET = "buyer_secret";
    public static final String SELLER_SECRET = "seller_secret";
    public static final String MSG_TEXT = "text";
    public static final int MAX_CONVERSATION_AGE_DAYS = 180;

    private PostBoxInitializer postBoxInitializer;

    @Mock
    private ConversationBlockRepository conversationBlockRepository;

    @Mock
    private PostBoxRepository postBoxRepository;

    @Mock
    private PostBoxInitializer.PostBoxWriteCallback postBoxWriteCallback;

    private ImmutableConversation.Builder convBuilder;
    private DateTime now;

    @Before
    public void setUp() throws Exception {
        postBoxInitializer = new PostBoxInitializer(postBoxRepository, conversationBlockRepository, 250, MAX_CONVERSATION_AGE_DAYS);

        now = DateTime.now(UTC);
        convBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId(CONV_ID)
                .withAdId(AD_ID)
                .withBuyer(BUYER_EMAIL, BUYER_SECRET)
                .withSeller(SELLER_EMAIL, SELLER_SECRET)
                .withCreatedAt(now)
                .withLastModifiedAt(now)
                .withState(ConversationState.ACTIVE)
                .withMessage(
                        ImmutableMessage.Builder
                                .aMessage()
                                .withId("msgId")
                                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                                .withFilterResultState(FilterResultState.OK)
                                .withHumanResultState(ModerationResultState.UNCHECKED)
                                .withReceivedAt(now)
                                .withLastModifiedAt(now)
                                .withPlainTextBody(MSG_TEXT)
                                .withState(MessageState.SENT)
                );
    }

    @Test
    public void newPostBox_newThreadCreated() throws Exception {
        Conversation conversation = convBuilder.build();

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList());

        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        ArgumentCaptor<PostBox> argumentCaptor = ArgumentCaptor.forClass(PostBox.class);
        verify(postBoxRepository).write(argumentCaptor.capture());

        assertThat(postbox.getNewRepliesCounter().getValue(), equalTo(1L));

        List<ConversationThread> conversationThreads = argumentCaptor.getValue().getConversationThreads();
        assertThat(conversationThreads.size(), equalTo(1));
        assertThat(conversationThreads.get(0).getConversationId(), equalTo(CONV_ID));
        assertThat(conversationThreads.get(0).getAdId(), equalTo(AD_ID));
        assertThat(conversationThreads.get(0).getCreatedAt(), equalTo(now));
        assertThat(conversationThreads.get(0).getReceivedAt(), equalTo(now));
        assertThat(conversationThreads.get(0).isContainsUnreadMessages(), equalTo(true));
        assertThat(conversationThreads.get(0).getPreviewLastMessage(), equalTo(Optional.of(MSG_TEXT)));
        assertThat(conversationThreads.get(0).getBuyerId(), equalTo(Optional.of(BUYER_EMAIL)));
        assertThat(conversationThreads.get(0).getMessageDirection(), equalTo(Optional.of(MessageDirection.BUYER_TO_SELLER.name())));
    }

    @Test
    public void postboxContainsThreadsThatAreTooOld_oldThreadsPurged() throws Exception {
        DateTime tooLongAgo = new DateTime(UTC).minusDays(MAX_CONVERSATION_AGE_DAYS);
        Conversation conversation = convBuilder.build();

        ConversationThread oldConversationThread = new ConversationThread(
                "oldAdId", "oldConversationId", tooLongAgo, now, now, false,
                Optional.of(MSG_TEXT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(BUYER_EMAIL),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name()));

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(oldConversationThread));

        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        ArgumentCaptor<PostBox> argumentCaptor = ArgumentCaptor.forClass(PostBox.class);
        verify(postBoxRepository).write(argumentCaptor.capture());

        assertThat(postbox.getNewRepliesCounter().getValue(), equalTo(1L));

        List<ConversationThread> conversationThreads = argumentCaptor.getValue().getConversationThreads();
        assertThat(conversationThreads.size(), equalTo(1));
        assertThat(conversationThreads.get(0).getConversationId(), equalTo(CONV_ID));
    }

    @Test
    public void postboxGetsNewConversationWithEmptyMessage_conversationNotAdded() throws Exception {
        ImmutableConversation conversation = convBuilder
                .withMessages(ImmutableList.of(ImmutableMessage.Builder
                        .aMessage()
                        .withId("msgId")
                        .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                        .withFilterResultState(FilterResultState.OK)
                        .withHumanResultState(ModerationResultState.UNCHECKED)
                        .withReceivedAt(now)
                        .withLastModifiedAt(now)
                        .withPlainTextBody("")
                        .withState(MessageState.SENT)
                        .build()))
                .build();

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList());

        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());

        assertThat(postbox.getNewRepliesCounter().getValue(), equalTo(0L));
        List<ConversationThread> conversationThreads = postbox.getConversationThreads();
        assertThat(conversationThreads, empty());
    }

    @Test
    public void postboxGetsEmptyMessageForExistingConversation_emptyMessageIgnored() throws Exception {
        ImmutableConversation conversation = convBuilder
                .withMessage(ImmutableMessage.Builder
                        .aMessage()
                        .withId("msgId")
                        .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                        .withFilterResultState(FilterResultState.OK)
                        .withHumanResultState(ModerationResultState.UNCHECKED)
                        .withReceivedAt(now)
                        .withLastModifiedAt(now)
                        .withPlainTextBody("")
                        .withState(MessageState.SENT))
                .build();

        ConversationThread existingThread = new ConversationThread(
                AD_ID, CONV_ID, now, now, now, false,
                Optional.of(MSG_TEXT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(BUYER_EMAIL),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name())
        );

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(existingThread));

        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());

        assertThat(postbox.getNewRepliesCounter().getValue(), equalTo(0L));
        List<ConversationThread> conversationThreads = postbox.getConversationThreads();
        assertThat(conversationThreads.size(), equalTo(1));
    }

    @Test
    public void buyerBlockedSeller_postboxNotUpdated() throws Exception {
        ImmutableConversation conversation = convBuilder
                .withMessages(ImmutableList.of(ImmutableMessage.Builder
                        .aMessage()
                        .withId("msgId")
                        .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                        .withFilterResultState(FilterResultState.OK)
                        .withHumanResultState(ModerationResultState.UNCHECKED)
                        .withReceivedAt(now)
                        .withLastModifiedAt(now)
                        .withPlainTextBody("Hi everybody")
                        .withState(MessageState.SENT)
                        .build()))
                .build();

        ConversationThread existingThread = new ConversationThread(
                AD_ID, CONV_ID, now, now, now, false,
                Optional.of(MSG_TEXT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(BUYER_EMAIL),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name())
        );

        PostBox postbox = new PostBox(BUYER_EMAIL, new Counter(), Lists.newArrayList(existingThread));
        when(postBoxRepository.byId(BUYER_EMAIL)).thenReturn(postbox);

        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(new ConversationBlock(CONV_ID, 1, Optional.of(new DateTime(DateTimeZone.UTC)), Optional.empty()));

        postBoxInitializer.moveConversationToPostBox(BUYER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());
    }

    @Test
    public void sellerBlockedBuyer_postboxNotUpdated() throws Exception {
        ImmutableConversation conversation = convBuilder
                .withMessages(ImmutableList.of(ImmutableMessage.Builder
                        .aMessage()
                        .withId("msgId")
                        .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                        .withFilterResultState(FilterResultState.OK)
                        .withHumanResultState(ModerationResultState.UNCHECKED)
                        .withReceivedAt(now)
                        .withLastModifiedAt(now)
                        .withPlainTextBody("Hi everybody")
                        .withState(MessageState.SENT)
                        .build()))
                .build();

        ConversationThread existingThread = new ConversationThread(
                AD_ID, CONV_ID, now, now, now, false,
                Optional.of(MSG_TEXT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(BUYER_EMAIL),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name())
        );

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(existingThread));
        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(new DateTime(DateTimeZone.UTC))));

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());
    }
}
