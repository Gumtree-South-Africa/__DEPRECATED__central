package com.gumtree.replyts2.plugins.reporting;

public interface EventPublisher {

    void publish(MessageProcessedEvent event);
}
