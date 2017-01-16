package com.ecg.replyts.app;

import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.app.eventpublisher.EventSerializer;
import com.ecg.replyts.core.api.model.user.event.UserEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static com.google.common.collect.Lists.newArrayList;

@Component
@ConditionalOnExpression("${replyts.user.event.publisher.enabled:false}")
public class UserEventListener {
    @Autowired
    private EventPublisher eventPublisher;

    private EventSerializer serializer = new EventSerializer();

    public void eventTriggered(UserEvent event) {
        byte[] data = serializer.serialize(event);
        eventPublisher.publishUserEvents(newArrayList(new EventPublisher.Event(null, data)));
    }
}
