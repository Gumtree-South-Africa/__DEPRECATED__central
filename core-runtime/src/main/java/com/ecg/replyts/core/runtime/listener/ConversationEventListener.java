package com.ecg.replyts.core.runtime.listener;


import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;

import java.util.List;

public interface ConversationEventListener {

    /**
     * Notification of the commit of some events.
     *
     * @param conversation the conversation (events are already applied)
     * @param conversationEvents the events that occurred for the conversation
     */
    void eventsTriggered(Conversation conversation, List<ConversationEvent> conversationEvents);

    /**
     * @return order value, listeners with a lower values are called before listeners with higher values
     *   {@value 200} is returned by default.
     */
    default int getOrder() {
        return 200;
    }
}
