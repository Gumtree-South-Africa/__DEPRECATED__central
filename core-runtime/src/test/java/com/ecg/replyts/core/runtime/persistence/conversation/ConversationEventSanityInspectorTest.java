package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import com.ecg.replyts.core.api.model.conversation.event.MessageModeratedEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConversationEventSanityInspectorTest {

    @Test
    public void acceptsNormalConversation() {
        assertEquals(2, filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.UNDECIDED, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, Maps.<String, String>newHashMap(), "", Collections.<String>emptyList())
        ).size());
    }

    @Test
    public void acceptsNormalConversationWithAgentModeration() {
        assertEquals(3, filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.HELD, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, Maps.<String, String>newHashMap(), "", Collections.<String>emptyList()),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.GOOD, "me")
        ).size());
    }

    @Test
    public void rejectsDoubleModerationToGood() {
        assertEquals(3, filter(
                new ConversationCreatedEvent("123", "adid", "buyer@host.com", "seller@host.com", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap()),
                new MessageAddedEvent("msg123", MessageDirection.BUYER_TO_SELLER, DateTime.now(), MessageState.HELD, "msgid", "msgidresp", FilterResultState.OK, ModerationResultState.UNCHECKED, Maps.<String, String>newHashMap(), "", Collections.<String>emptyList()),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.GOOD, "me"),
                new MessageModeratedEvent("msg123", DateTime.now(), ModerationResultState.GOOD, "me")
        ).size());
    }

    private List<ConversationEvent> filter(ConversationEvent... ev) {
        return ConversationEventSanityInspector.disposeEventsThatLeadToUnresolvableStates(new ConversationEvents(Lists.newArrayList(ev))).getEvents();
    }
}
