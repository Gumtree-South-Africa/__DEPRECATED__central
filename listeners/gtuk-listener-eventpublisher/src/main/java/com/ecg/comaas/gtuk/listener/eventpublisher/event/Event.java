package com.ecg.comaas.gtuk.listener.eventpublisher.event;

public abstract class Event {

    protected final String eventName;

    protected Event(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public abstract String toJsonString();

    public abstract String getEventLoggerFriendly();
}
