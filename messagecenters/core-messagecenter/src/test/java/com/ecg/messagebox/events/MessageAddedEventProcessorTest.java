package com.ecg.messagebox.events;

import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.model.conversation.ProcessingFeedbackBuilder;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MessageAddedEventProcessorTest {

    @Mock
    private Conversation conv;
    @Mock
    private Message msg;
    @Mock
    private MessageAddedKafkaPublisher publisher;
    @Mock
    private EventConverter converter;
    @Captor
    private ArgumentCaptor<List<ConversationEvent>> convEventsCaptor;
    @Captor
    private ArgumentCaptor<UserUnreadCounts> unreadCountsArgumentCaptor;

    @Test
    public void disabledByDefault() {
        MessageAddedEventProcessor processor = new MessageAddedEventProcessor(converter, publisher, false);
        processor.publishMessageAddedEvent(conv, msg, "test 123", new UserUnreadCounts("1", 1, 1));
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void publishMessage() throws ClassNotFoundException {
        MessageAddedEventProcessor processor = new MessageAddedEventProcessor(converter, publisher, true);

        when(converter.toEvents(any(), any(), any(), anyBoolean())).thenReturn(Lists.newArrayList(new EventPublisher.Event(null, null)));

        ImmutableMessage.Builder msgBuilder = ImmutableMessage.Builder.aMessage()
                .withMessageDirection(BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.GOOD)
                .withHeaders(new HashMap<>())
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                        .withFilterName("filter1")
                        .withFilterInstance("filter1"))
                .withTextParts(Lists.newArrayList())
                .withId("id");


        processor.publishMessageAddedEvent(
                ImmutableConversation.Builder.aConversation()
                        .withCreatedAt(new DateTime())
                        .withLastModifiedAt(new DateTime())
                        .withState(ConversationState.ACTIVE)
                        .withMessage(msgBuilder)
                        .build(),
                msgBuilder.build(), "test 123", new UserUnreadCounts("1", 1, 1));

        Mockito.verify(converter).toEvents(any(), convEventsCaptor.capture(), unreadCountsArgumentCaptor.capture(), anyBoolean());
        List<ConversationEvent> addedEvent = convEventsCaptor.getValue();
        MessageAddedEvent m = (MessageAddedEvent) addedEvent.get(0);
        assertEquals("id", m.getMessageId());
        assertEquals(MessageState.SENT, m.getState());
        assertEquals("test 123", m.getTextParts().get(0));
        assertEquals("test 123", m.getHeaders().get("X-User-Message"));

        UserUnreadCounts uc = unreadCountsArgumentCaptor.getValue();
        assertEquals(1, uc.getNumUnreadConversations());
        assertEquals(1, uc.getNumUnreadMessages());
        assertEquals("1", uc.getUserId());
    }

    @Test
    public void newConnection() {
        MessageAddedEventProcessor processor = new MessageAddedEventProcessor(converter, publisher, true);

        List<Message> connected1 = new ArrayList<Message>() {{
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(BUYER_TO_SELLER));
        }};
        assertTrue(processor.isNewConnection(connected1));

        List<Message> connected2 = new ArrayList<Message>() {{
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(BUYER_TO_SELLER));
        }};
        assertTrue(processor.isNewConnection(connected2));

        List<Message> notConnected1 = new ArrayList<>();
        assertFalse(processor.isNewConnection(notConnected1));

        List<Message> notConnected2 = new ArrayList<Message>() {{
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
        }};
        assertFalse(processor.isNewConnection(notConnected2));

        List<Message> notConnected3 = new ArrayList<Message>() {{
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(SELLER_TO_BUYER));
            add(dummyMessage(BUYER_TO_SELLER));
            add(dummyMessage(BUYER_TO_SELLER));
        }};
        assertFalse(processor.isNewConnection(notConnected3));
    }

    private Message dummyMessage(MessageDirection direction) {
        return ImmutableMessage.Builder.aMessage()
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.GOOD)
                .withHeaders(new HashMap<>())
                .withProcessingFeedback(new ArrayList<>())
                .withMessageDirection(direction).build();
    }
}
