package com.ecg.replyts2.eventpublisher.kafka;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.app.eventpublisher.MessageReceivedListener;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.util.Properties;

/**
 * Conditionally configure event publishing to Kafka.
 *
 * Enable publishing by adding the following to the {@code replyts.properties}:
 *
 * <pre>{@code
 * replyts.event.publisher.kafka.enabled = true
 * replyts.kafka.broker.list = host1:9092,host2:9092
 * replyts.kafka.topic = conversations
 * }
 * </pre>
 */
@Configuration
public class KafkaEventPublisherConfig {

    @Value("${replyts.event.publisher.kafka.enabled:false}")
    private boolean kafkaEnabled;
    @Value("${replyts.kafka.broker.list:localhost:9092}")
    private String kafkaBrokers;
    @Value("${replyts.kafka.topic:conversations}")
    private String kafkaTopic;

    @Autowired
    private MailCloakingService mailCloakingService;

    private Producer<String, byte[]> producer;

    @Bean
    @Conditional(KafkaEnabledConditional.class)
    public MessageReceivedListener kafkaMessageReceivedListener() {
        return new MessageReceivedListener(mailCloakingService, newkafkaEventPublisher());
    }

    private EventPublisher newkafkaEventPublisher() {
        Assert.hasLength(kafkaBrokers);

        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaBrokers);
        props.put("key.serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(props);
        producer = new Producer<>(config);

        return new KafkaEventPublisher(producer, kafkaTopic);
    }

    @PreDestroy
    public void close() {
        if (producer != null) producer.close();
        producer = null;
    }

    public static class KafkaEnabledConditional implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Boolean.parseBoolean(context.getEnvironment().getProperty("replyts.event.publisher.kafka.enabled", "false"));
        }
    }
}
