package com.ecg.replyts.app.eventpublisher;

import java.util.List;

public interface EventPublisher {

    void publishConversationEvents(List<Event> events);

    void publishUserEvents(List<Event> events);

    class Event {
        public final String partitionKey;
        public final byte[] data;

        public Event(String partitionKey, byte[] data) {
            this.partitionKey = partitionKey;
            this.data = data;
        }
    }
}
