package com.ecg.comaas.gtuk.listener.reporting;

public interface EventPublisher {

    void publish(MessageProcessedEvent event);
}
