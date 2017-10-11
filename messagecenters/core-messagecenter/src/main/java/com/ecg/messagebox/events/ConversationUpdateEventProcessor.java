package com.ecg.messagebox.events;

import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

@Component
public class ConversationUpdateEventProcessor {

    @Value("${replyts.convupdate.publisher.enabled:false}")
    private boolean enabled;
    private ConversationUpdateKafkaPublisher publisher;
    private EventConverter eventConverter;
    private static final String USER_MESSAGE_HEADER = "X-User-Message";

    @Autowired
    public ConversationUpdateEventProcessor(MailCloakingService mailCloaking) {
        this.eventConverter = new EventConverter(mailCloaking);
    }

    public ConversationUpdateEventProcessor(EventConverter eventConverter,
                                            ConversationUpdateKafkaPublisher publisher, boolean enabled) {
        this.enabled = enabled;
        this.eventConverter = eventConverter;
        this.publisher = publisher;
    }

    public void publishConversationUpdate(Conversation conv, Message msg, String msgText) {
        if (enabled) {
            publisher.publishNewMessage(constructEvent(conv, msg, msgText));
        }
    }

    private EventPublisher.Event constructEvent(Conversation conv, Message msg, String msgText) {
        final Map<String, String> headers = new HashMap<>(msg.getHeaders());
        headers.put(USER_MESSAGE_HEADER, msgText);

        final MessageAddedEvent messageAddedEvent = new MessageAddedEvent(msg.getId(),
                msg.getMessageDirection(),
                msg.getReceivedAt(),
                msg.getState(),
                msg.getSenderMessageIdHeader(),
                msg.getInResponseToMessageId(),
                FilterResultState.OK,
                ModerationResultState.GOOD,
                headers,
                msgText,
                msg.getAttachmentFilenames(),
                singletonList(msgText));

        return eventConverter.toEvents(conv, newArrayList(messageAddedEvent)).get(0);
    }

    @Autowired(required = false)
    public void setPublisher(@Qualifier("convUpdateKafkaPublisher") ConversationUpdateKafkaPublisher publisher) {
        this.publisher = publisher;
    }
}