package com.ecg.replyts.app;

import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.app.eventpublisher.EventSerializer;
import com.ecg.replyts.core.api.model.user.event.UserEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.collect.Lists.newArrayList;

public class UserEventListener {

    @Autowired(required = false)
    private EventPublisher eventPublisher;
    private EventSerializer serializer = new EventSerializer();
    @Value("${replyts.user.event.publisher.enabled:false}")
    private boolean userEventsEnabled;

    public void eventTriggered(UserEvent event) {
        if (eventPublisher != null && userEventsEnabled) {
            byte[] data = serializer.serialize(event);
            eventPublisher.publishUserEvents(newArrayList(new EventPublisher.Event(null, data)));
        }
    }

}
