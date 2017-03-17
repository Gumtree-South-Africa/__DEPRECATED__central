package com.gumtree.replyts2.eventpublisher.publisher;

import com.gumtree.replyts2.eventpublisher.event.Event;

public interface EventPublisher {

    public void publishEvent(Event event);

}
