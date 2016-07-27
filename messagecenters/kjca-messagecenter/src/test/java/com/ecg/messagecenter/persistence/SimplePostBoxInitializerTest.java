package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SimplePostBoxInitializerTest.TestContext.class)
@TestPropertySource(properties = {
  "replyts.maxPreviewMessageCharacters = 250",
  "replyts.maxConversationAgeDays = 180"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SimplePostBoxInitializerTest {
    public static final String SELLER_EMAIL = "seller@example.com";
    public static final String AD_ID = "adid";
    public static final String CONV_ID = "convId";
    public static final String BUYER_EMAIL = "buyer@example.com";
    public static final String BUYER_SECRET = "buyer_secret";
    public static final String SELLER_SECRET = "seller_secret";
    public static final String MSG_TEXT = "text";

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private ConversationBlockRepository conversationBlockRepository;

    @Autowired
    private SimplePostBoxInitializer postBoxInitializer;

    private SimplePostBoxInitializer.PostBoxWriteCallback postBoxWriteCallback = mock(SimplePostBoxInitializer.PostBoxWriteCallback.class);

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
    public void newPostBox_newThreadCreated() throws Exception {
        Conversation conversation = convBuilder.build();

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(), 180);

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
        DateTime tooLongAgo = new DateTime(UTC).minusDays(181);
        Conversation conversation = convBuilder.build();

        ConversationThread oldConversationThread = new ConversationThread(
                "oldAdId", "oldConversationId", tooLongAgo, now, now, false,
                Optional.of(MSG_TEXT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(BUYER_EMAIL),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name()));

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(oldConversationThread), 180);

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
                        .withTextParts(ImmutableList.of(""))
                        .withState(MessageState.SENT)
                        .build()))
                .build();

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(), 180);

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

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(existingThread), 180);

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

        PostBox postbox = new PostBox(BUYER_EMAIL, new Counter(), Lists.newArrayList(existingThread), 180);
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

        PostBox postbox = new PostBox(SELLER_EMAIL, new Counter(), Lists.newArrayList(existingThread), 180);
        when(postBoxRepository.byId(SELLER_EMAIL)).thenReturn(postbox);

        when(conversationBlockRepository.byConversationId(CONV_ID)).thenReturn(new ConversationBlock(CONV_ID, 1, Optional.empty(), Optional.of(new DateTime(DateTimeZone.UTC))));

        postBoxInitializer.moveConversationToPostBox(SELLER_EMAIL, conversation, true, postBoxWriteCallback);

        verifyZeroInteractions(postBoxWriteCallback);
        verify(postBoxRepository, never()).write(any());
    }

    @Configuration
    static class TestContext {
        @Bean
        public ConversationBlockRepository conversationBlockRepository() {
            return mock(ConversationBlockRepository.class);
        }

        @Bean
        public SimplePostBoxRepository postBoxRepository() {
            return mock(SimplePostBoxRepository.class);
        }

        @Bean
        public SimplePostBoxInitializer postBoxInitializer() {
            return new SimplePostBoxInitializer();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}
