package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.ConversationEventListener;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class ConversationEventListeners {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationEventListeners.class);

    private static final Counter EVENT_LISTENER_ERRORS_COUNTER = TimingReports.newCounter("event-listener-errors");

    @Autowired(required = false)
    private List<ConversationEventListener> listeners = emptyList();

    @PostConstruct
    public void sortListeners() {
        this.listeners.sort(Comparator.comparingInt(ConversationEventListener::getOrder));
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
