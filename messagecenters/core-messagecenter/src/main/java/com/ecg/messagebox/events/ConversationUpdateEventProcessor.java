package com.ecg.messagebox.events;

import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.event.MessageAddedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

@Component
public class ConversationUpdateEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConversationUpdateEventProcessor.class);
    @Value("${replyts.convupdate.publisher.enabled:false}")
    private boolean enabled;
    @Value("${replyts.kafka.broker.list:localhost:9092}")
    private String brokers;
    @Value("${replyts.convupdate.kafka.topic:conversation_updates}")
    private String topic;
    private ConversationUpdateKafkaPublisher publisher;
    private EventConverter eventConverter;
    private static final String USER_MESSAGE_HEADER = "X-User-Message";

    @Autowired
    public ConversationUpdateEventProcessor(MailCloakingService mailCloaking) {
        init(() -> new EventConverter(mailCloaking),
                () -> new ConversationUpdateKafkaPublisher(createKafkaProducer(), topic));
    }

    public ConversationUpdateEventProcessor(Supplier<EventConverter> eventConverter,
                                            Supplier<ConversationUpdateKafkaPublisher> publisher, boolean enabled) {
        this.enabled = enabled;
        init(eventConverter, publisher);
    }

    void init(Supplier<EventConverter> eventConverter, Supplier<ConversationUpdateKafkaPublisher> publisher) {
        if (enabled) {
            log.info("Enabling submitting of the events to the kafka topic: {}", topic);
            this.eventConverter = eventConverter.get();
            this.publisher = publisher.get();
        } else {
            log.info("Disabled submitting of the events to the kafka topic: {}", topic);
        }
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

    private Producer<String, byte[]> createKafkaProducer() {
        Assert.hasLength(brokers);

        Properties props = new Properties();
        props.put("bootstrap.servers", brokers);
        props.put("key.serializer", "kafka.serializer.StringEncoder");
        props.put("acks", "1");

        return new KafkaProducer<>(props);
    }
}