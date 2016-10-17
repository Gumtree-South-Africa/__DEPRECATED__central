package com.ecg.replyts2.eventpublisher.kafka;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.app.eventpublisher.MessageReceivedListener;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

/**
 * Conditionally configure event publishing to Kafka.
 * <p>
 * Enable publishing by adding the following to the {@code replyts.properties}:
 * <p>
 * <pre>{@code
 * replyts.event.publisher.kafka.enabled = true
 * replyts.kafka.broker.list = host1:9092,host2:9092
 * replyts.kafka.topic = conversations
 * replyts.kafka.replay.topic = conversations_replay
 * }
 * </pre>
 */
@Configuration
public class KafkaEventPublisherConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisherConfig.class);

    @Value("${replyts.event.publisher.kafka.enabled:false}")
    private boolean kafkaEnabled;
    @Value("${replyts.kafka.broker.list:localhost:9092}")
    private String kafkaBrokers;
    @Value("${replyts.kafka.topic:conversations}")
    private String kafkaConversationEventsTopic;
    @Value("${replyts.kafka.user.events.topic:user_events}")
    private String kafkaUserEventsTopic;
    @Value("${replyts.kafka.replay.topic:conversations_replay}")
    private String kafkaReplayTopic;

    @Autowired
    private MailCloakingService mailCloakingService;

    private Producer<String, byte[]> producer;

    @PostConstruct
    @Conditional(KafkaEnabledConditional.class)
    private void createKafkaProducer() {
        if(!kafkaEnabled) {
            LOGGER.debug("Kafka Event Publisher is disabled!");
            return;
        }
        Assert.hasLength(kafkaBrokers);

        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaBrokers);
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<>(config);
    }

    @Bean
    @Conditional(KafkaEnabledConditional.class)
    public MessageReceivedListener kafkaMessageReceivedListener() {
        return new MessageReceivedListener(new EventConverter(mailCloakingService), newKafkaEventPublisher());
    }

    @Bean
    @Conditional(KafkaEnabledConditional.class)
    public EventPublisher newKafkaEventPublisher() {
        return new KafkaEventPublisher(producer, kafkaConversationEventsTopic, kafkaUserEventsTopic);
    }

    public EventPublisher newKafkaEventReplayPublisher() {
        return new KafkaEventPublisher(producer, kafkaReplayTopic, kafkaUserEventsTopic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
        producer = null;
    }

    public static class KafkaEnabledConditional implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return context.getEnvironment().getProperty("replyts.event.publisher.kafka.enabled", Boolean.class, false);
        }
    }
}