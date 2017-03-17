package com.gumtree.replyts2.eventpublisher.event;

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
