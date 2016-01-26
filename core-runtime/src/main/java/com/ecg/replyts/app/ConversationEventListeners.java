package com.ecg.replyts.app;


import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ConversationEventListeners {

    private static final Counter EVENT_LISTENER_ERRORS_COUNTER = TimingReports.newCounter("event-listener-errors");

    private static final Logger LOG = LoggerFactory.getLogger(ConversationEventListeners.class);

    @Autowired(required = false)
    private List<ConversationEventListener> listeners;

    public ConversationEventListeners() {
        listeners = ImmutableList.of();
    }

    public ConversationEventListeners(List<ConversationEventListener> listeners) {
        this.listeners = listeners;
    }

    public void processEventListeners(ImmutableConversation conversation, List<ConversationEvent> conversationEvents) {
            listeners.forEach(l -> {
                try {
                    l.eventsTriggered(conversation, conversationEvents);
                } catch(Exception e) {
                    LOG.error("Error while processing event listener {}", l, e);
                    EVENT_LISTENER_ERRORS_COUNTER.inc();
                }
            });
    }

}
