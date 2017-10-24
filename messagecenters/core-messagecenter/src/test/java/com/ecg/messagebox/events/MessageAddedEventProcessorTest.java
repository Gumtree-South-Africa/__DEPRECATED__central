package com.ecg.messagebox.events;

import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.model.conversation.ProcessingFeedbackBuilder;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MessageAddedEventProcessorTest {

    @Mock
    private MailCloakingService mailCloak;
    @Mock
    private Conversation conv;
    @Mock
    private Message msg;
    @Mock
    private MessageAddedKafkaPublisher publisher;
    @Mock
    private EventConverter converter;
    @Captor
    private ArgumentCaptor<List<ConversationEvent>> captor;


    @Test
    public void disabledByDefault() {
        MessageAddedEventProcessor processor = new MessageAddedEventProcessor(converter, publisher, false);
        processor.publishMessageAddedEvent(conv, msg, "test 123");
        verifyNoMoreInteractions(publisher);
    }

    @Test
    public void publishMessage() throws ClassNotFoundException {
        MessageAddedEventProcessor processor = new MessageAddedEventProcessor(converter, publisher, true);

        when(converter.toEvents(any(), any())).thenReturn(Lists.newArrayList(new EventPublisher.Event(null, null)));

        ImmutableMessage.Builder msgBuilder = ImmutableMessage.Builder.aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.GOOD)
                .withHeaders(new HashMap<>())
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                        .withFilterName("filter1")
                        .withFilterInstance("filter1"))
                .withLastEditor(Optional.empty())
                .withTextParts(Lists.newArrayList())
                .withId("id");


        processor.publishMessageAddedEvent(
                ImmutableConversation.Builder.aConversation()
                        .withCreatedAt(new DateTime())
                        .withLastModifiedAt(new DateTime())
                        .withState(ConversationState.ACTIVE)
                        .withMessage(msgBuilder)
                        .build(),
                msgBuilder.build(), "test 123");

        Mockito.verify(converter).toEvents(any(), captor.capture());
        List<ConversationEvent> addedEvent = captor.getValue();
        MessageAddedEvent m = (MessageAddedEvent) addedEvent.get(0);
        Assert.assertEquals("id", m.getMessageId());
        Assert.assertEquals(MessageState.SENT, m.getState());
        Assert.assertEquals("test 123", m.getTextParts().get(0));
        Assert.assertEquals("test 123", m.getHeaders().get("X-User-Message"));
    }

}
