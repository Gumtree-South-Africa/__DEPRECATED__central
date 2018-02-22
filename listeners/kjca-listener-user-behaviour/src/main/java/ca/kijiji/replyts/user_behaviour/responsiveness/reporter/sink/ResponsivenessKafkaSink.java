package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.UserResponsivenessListener;
import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("#{'${user-behaviour.responsiveness.sink:fs}' == 'queue'}")
@ConditionalOnBean(UserResponsivenessListener.class)
public class ResponsivenessKafkaSink implements ResponsivenessSink {

    private static final Logger LOG = LoggerFactory.getLogger(ResponsivenessKafkaSink.class);

    @Value("${user-behaviour.responsiveness.queue.topic:userresponsiveness_ca}")
    private String queueTopic;

    private final ResponsivenessKafkaProducer responsivenessKafkaProducer;

    @Autowired
    public ResponsivenessKafkaSink(ResponsivenessKafkaProducer responsivenessKafkaProducer) {
        this.responsivenessKafkaProducer = responsivenessKafkaProducer;
    }

    @Override
    public void storeRecord(String writerId, ResponsivenessRecord record) {
        // skipping writerId, it is not needed for Kafka
        responsivenessKafkaProducer.getProducer().send(new ProducerRecord<>(queueTopic, record), (recordMetadata, e) -> {
            if (e != null) {
                LOG.error("Error storing responsiveness record {} with writerId {}", record, writerId, e);
            }
        });
    }
}
