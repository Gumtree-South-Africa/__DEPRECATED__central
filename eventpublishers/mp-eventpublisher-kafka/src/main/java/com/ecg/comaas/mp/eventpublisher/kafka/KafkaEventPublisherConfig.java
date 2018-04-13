package com.ecg.comaas.mp.eventpublisher.kafka;

import com.ecg.comaas.mp.eventpublisher.kafka.cronjob.KafkaEventReplayCronJobConfig;
import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.ecg.replyts.app.eventpublisher.MessageReceivedListener;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

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
@ComaasPlugin
@Configuration
@Import(KafkaEventReplayCronJobConfig.class)
public class KafkaEventPublisherConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisherConfig.class);

    @Value("${replyts.event.publisher.kafka.enabled:false}")
    private boolean kafkaEnabled;
    @Value("${replyts.kafka.broker.list:#{null}}")
    private String kafkaBrokers;
    @Value("${replyts.kafka.topic:conversations}")
    private String kafkaConversationEventsTopic;
    @Value("${replyts.kafka.user.events.topic:user_events}")
    private String kafkaUserEventsTopic;
    @Value("${replyts.kafka.replay.topic:conversations_replay}")
    private String kafkaReplayTopic;

    @Autowired
    private MailCloakingService mailCloakingService;

    private KafkaProducer<String, byte[]> producer;

    @PostConstruct
    @Conditional(KafkaEnabledConditional.class)
    private void createKafkaProducer() {
        if(!kafkaEnabled) {
            LOGGER.debug("Kafka Event Publisher is disabled!");
            return;
        }
        checkNotNull(kafkaBrokers);

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArraySerializer.class.getName());
        props.put(ACKS_CONFIG, "1");

        producer = new KafkaProducer<>(props);
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