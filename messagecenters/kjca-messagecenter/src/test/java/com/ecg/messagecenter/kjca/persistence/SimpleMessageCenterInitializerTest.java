package com.ecg.messagecenter.kjca.persistence;

import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.Counter;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlock;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SimpleMessageCenterInitializerTest {
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final PostBoxId SELLER_POSTBOX_ID = PostBoxId.fromEmail(SELLER_EMAIL);
    private static final String AD_ID = "adid";
    private static final String CONV_ID = "convId";
    private static final String BUYER_EMAIL = "buyer@example.com";
    private static final String BUYER_SECRET = "buyer_secret";
    private static final String SELLER_SECRET = "seller_secret";
    private static final String MSG_TEXT = "text";

    private SimpleMessageCenterInitializer.PostBoxWriteCallback postBoxWriteCallback = mock(SimpleMessageCenterInitializer.PostBoxWriteCallback.class);

    private ImmutableConversation.Builder convBuilder;
    private DateTime now;

    @Before
    public void setUp() throws Exception {
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
                                .withTextParts(ImmutableList.of(MSG_TEXT))
                                .withState(MessageState.SENT)
                );
    }

    @Test
    public void newPostBox_newThreadCreated() {
        Conversation conversation = convBuilder.build();

        SimpleMessageCenterRepository postBoxRepository = mock(SimpleMessageCenterRepository.class);
        when(postBoxRepository.threadById(SELLER_POSTBOX_ID, conversation.getId())).thenReturn(Optional.empty());
        when(postBoxRepository.upsertThread(eq(SELLER_POSTBOX_ID), any(AbstractConversationThread.class), eq(true))).thenReturn(1L);

        TextAnonymizer textAnonymizer = mock(TextAnonymizer.class);
        when(textAnonymizer.anonymizeText(any(Conversation.class), anyString())).thenReturn(MSG_TEXT);

        ConversationBlockRepository conversationBlockRepository = mock(ConversationBlockRepository.class);

        SimpleMessageCenterInitializer postBoxInitializer = new SimpleMessageCenterInitializer(postBoxRepository, conversationBlockRepository, textAnonymizer);
        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        ArgumentCaptor<AbstractConversationThread> argumentCaptor = ArgumentCaptor.forClass(AbstractConversationThread.class);
        verify(postBoxRepository).upsertThread(eq(SELLER_POSTBOX_ID), argumentCaptor.capture(), eq(true));

        AbstractConversationThread conversationThread = argumentCaptor.getValue();

        assertThat(conversationThread.getConversationId(), equalTo(CONV_ID));
        assertThat(conversationThread.getAdId(), equalTo(AD_ID));
        assertThat(conversationThread.getCreatedAt(), equalTo(now));
        assertThat(conversationThread.getReceivedAt(), equalTo(now));
        assertThat(conversationThread.isContainsUnreadMessages(), equalTo(true));
        assertThat(conversationThread.getPreviewLastMessage(), equalTo(Optional.of(MSG_TEXT)));
        assertThat(conversationThread.getBuyerId(), equalTo(Optional.of(BUYER_EMAIL)));
        assertThat(conversationThread.getMessageDirection(), equalTo(Optional.of(MessageDirection.BUYER_TO_SELLER.name())));
    }

    // We don't check for old-thread-purging behavior anymore, as this was only relevant when (re)writing entire PostBoxes

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
                        .withTextParts(ImmutableList.of(""))
                        .withState(MessageState.SENT)
                        .build()))
                .build();

        PostBox postbox = new PostBox<>(SELLER_EMAIL, new Counter(), Lists.newArrayList());

        SimpleMessageCenterRepository postBoxRepository = mock(SimpleMessageCenterRepository.class);
        when(postBoxRepository.byId(SELLER_POSTBOX_ID)).thenReturn(postbox);

        TextAnonymizer textAnonymizer = mock(TextAnonymizer.class);
        when(textAnonymizer.anonymizeText(any(Conversation.class), anyString())).thenReturn(MSG_TEXT);

        ConversationBlockRepository conversationBlockRepository = mock(ConversationBlockRepository.class);

        SimpleMessageCenterInitializer postBoxInitializer = new SimpleMessageCenterInitializer(postBoxRepository, conversationBlockRepository, textAnonymizer);
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
                        .withTextParts(ImmutableList.of(""))
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

        SimpleMessageCenterRepository postBoxRepository = mock(SimpleMessageCenterRepository.class);
        when(postBoxRepository.threadById(SELLER_POSTBOX_ID, conversation.getId())).thenReturn(Optional.of(existingThread));

        TextAnonymizer textAnonymizer = mock(TextAnonymizer.class);
        when(textAnonymizer.anonymizeText(any(Conversation.class), anyString())).thenReturn(MSG_TEXT);

        ConversationBlockRepository conversationBlockRepository = mock(ConversationBlockRepository.class);

        SimpleMessageCenterInitializer postBoxInitializer = new SimpleMessageCenterInitializer(postBoxRepository, conversationBlockRepository, textAnonymizer);
        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
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
                        .withTextParts(ImmutableList.of("Hi everybody"))
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

        PostBox postbox = new PostBox<>(BUYER_EMAIL, new Counter(), Lists.newArrayList(existingThread));
        SimpleMessageCenterRepository postBoxRepository = mock(SimpleMessageCenterRepository.class);
        when(postBoxRepository.byId(PostBoxId.fromEmail(BUYER_EMAIL))).thenReturn(postbox);

        TextAnonymizer textAnonymizer = mock(TextAnonymizer.class);
        when(textAnonymizer.anonymizeText(any(Conversation.class), anyString())).thenReturn(MSG_TEXT);

        ConversationBlockRepository conversationBlockRepository = mock(ConversationBlockRepository.class);
        when(conversationBlockRepository.byId(CONV_ID)).thenReturn(new ConversationBlock(CONV_ID, 1, Optional.of(new DateTime(DateTimeZone.UTC)), Optional.empty()));

        SimpleMessageCenterInitializer postBoxInitializer = new SimpleMessageCenterInitializer(postBoxRepository, conversationBlockRepository, textAnonymizer);
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
                        .withTextParts(ImmutableList.of("Hi everybody"))
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

        PostBox postbox = new PostBox<>(SELLER_EMAIL, new Counter(), Lists.newArrayList(existingThread));
        SimpleMessageCenterRepository postBoxRepository = mock(SimpleMessageCenterRepository.class);
        when(postBoxRepository.byId(SELLER_POSTBOX_ID)).thenReturn(postbox);

        TextAnonymizer textAnonymizer = mock(TextAnonymizer.class);
        when(textAnonymizer.anonymizeText(any(Conversation.class), anyString())).thenReturn(MSG_TEXT);

        ConversationBlockRepository conversationBlockRepository = mock(ConversationBlockRepository.class);
        when(conversationBlockRepository.byId(CONV_ID)).thenReturn(new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(new DateTime(DateTimeZone.UTC))));

        SimpleMessageCenterInitializer postBoxInitializer = new SimpleMessageCenterInitializer(postBoxRepository, conversationBlockRepository, textAnonymizer);
        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());
    }
}
