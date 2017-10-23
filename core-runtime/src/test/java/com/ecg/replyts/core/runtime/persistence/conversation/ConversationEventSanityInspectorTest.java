package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageModeratedEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class ConversationEventSanityInspectorTest {

    @Test
    public void acceptsNormalConversation() {
        assertEquals(2, filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.UNDECIDED, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, newHashMap(), "", emptyList(), emptyList())
        ).size());
    }

    @Test
    public void acceptsNormalConversationWithAgentModeration() {
        assertEquals(3, filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.HELD, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, newHashMap(), "", emptyList(), emptyList()),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.GOOD, "me")
        ).size());
    }

    @Test
    public void ignoreDoubleModerationEvents() {
        List<ConversationEvent> conversationEvents = filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.HELD, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, newHashMap(), "", emptyList(), emptyList()),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.GOOD, "me"),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.BAD, "me"));

        // Duplicate event is ignored and does not fail processing.
        assertEquals(4, conversationEvents.size());

        // Check whether the first event was applied and the second one ignored.
        Conversation conversation = ImmutableConversation.replay(conversationEvents);
        Message message = conversation.getMessages().get(0);
        assertEquals(message.getState(), MessageState.SENT);
    }

    private List<ConversationEvent> filter(ConversationEvent... ev) {
        return ConversationEventSanityInspector.disposeEventsThatLeadToUnresolvableStates(new ConversationEvents(Lists.newArrayList(ev))).getEvents();
    }
}
