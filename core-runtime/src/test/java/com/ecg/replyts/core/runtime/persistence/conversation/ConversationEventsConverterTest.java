package com.ecg.replyts.core.runtime.persistence.conversation;


import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.cap.VClock;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.ConversationState.ACTIVE;
import static com.ecg.replyts.core.api.model.conversation.FilterResultState.OK;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.ModerationResultState.UNCHECKED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationEventsConverterTest {

    @Mock
    private ConversationEvents events;

    @Mock
    private VClock vclock;

    private final ConversationCreatedEvent createdEvent = new ConversationCreatedEvent("1", "2", "3", "4", "buyersecret", "sellersecret", DateTime.now(), ACTIVE, Maps.<String, String>newHashMap());

    @Mock
    private ConversationJsonSerializer serializer;

    @Test
    public void applyingJsonSerializationAndJsonDeserializationKeepsConversationIntegrity() {

        DateTime now = DateTime.now();
        ConversationEventsConverter converter = new ConversationEventsConverter("testBucketName", new ConversationJsonSerializer());
        Map<String, String> customValues = new HashMap<String, String>();
        customValues.put("abc", "123");
        customValues.put("def", "456");
        ConversationCreatedEvent conversationCreatedEvent = new ConversationCreatedEvent("1", "2", "3", "4", "buyersecret", "sellersecret", now, ACTIVE, customValues);

        MessageAddedEvent messageAddedEvent = new MessageAddedEvent("5", BUYER_TO_SELLER, now, MessageState.SENT, "<ABC@platform.com>", "1:1", OK, UNCHECKED, Collections.<String, String>emptyMap(), "body", Collections.<String>emptyList());

        List<ConversationEvent> domainObject = new ArrayList<ConversationEvent>();
        domainObject.add(conversationCreatedEvent);
        domainObject.add(messageAddedEvent);

        IRiakObject riakObject = converter.fromDomain(new ConversationEvents(domainObject), vclock);

        List<ConversationEvent> deserialized = converter.toDomain(riakObject).getEvents();

        assertThat(deserialized.size(), is(2));

        conversationCreatedEvent = (ConversationCreatedEvent) deserialized.get(0);
        messageAddedEvent = (MessageAddedEvent) deserialized.get(1);

        assertThat(conversationCreatedEvent.getConversationId(), is("1"));
        assertThat(conversationCreatedEvent.getAdId(), is("2"));
        assertThat(conversationCreatedEvent.getBuyerId(), is("3"));
        assertThat(conversationCreatedEvent.getSellerId(), is("4"));
        assertThat(conversationCreatedEvent.getBuyerSecret(), is("buyersecret"));
        assertThat(conversationCreatedEvent.getSellerSecret(), is("sellersecret"));
        assertThat(conversationCreatedEvent.getCustomValues().get("abc"), is("123"));
        assertThat(conversationCreatedEvent.getCustomValues().get("def"), is("456"));
        assertThat(conversationCreatedEvent.getState(), is(ACTIVE));

        assertThat(messageAddedEvent.getMessageId(), is("5"));
        assertThat(messageAddedEvent.getMessageDirection(), is(BUYER_TO_SELLER));
        assertThat(messageAddedEvent.getSenderMessageIdHeader(), is("<ABC@platform.com>"));
        assertThat(messageAddedEvent.getInResponseToMessageId(), is("1:1"));
        assertThat(messageAddedEvent.getFilterResultState(), is(OK));
        assertThat(messageAddedEvent.getHumanResultState(), is(UNCHECKED));
        assertThat(messageAddedEvent.getHeaders().isEmpty(), is(true));
        assertThat(messageAddedEvent.getPlainTextBody(), is("body"));
    }


    @Test(expected = IllegalArgumentException.class)
    public void preventStoringIfAllowedSizeExceeded() throws IOException {

        ConversationEventsConverter converter = new ConversationEventsConverter("testBucketName", serializer, ValueSizeConstraint.maxMb(0));

        when(events.getEvents()).thenReturn(Arrays.asList((ConversationEvent) createdEvent));
        when(serializer.serialize(anyList())).thenReturn(new byte[40]);

        converter.fromDomain(events, vclock);
    }
}
