package com.ecg.messagebox.consumer;

import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsumerConfiguration {

    @Value("${kafka.core.servers}")
    String bootstrapServers;

    @Value("${replyts.tenant.short}")
    private String shortTenantName;

    @Value("${kafka.core.servers}")
    String kafkaServers;

    @Autowired
    private CassandraPostBoxRepository postBoxRepository;

    @Bean(initMethod = "start", destroyMethod = "close")
    public KafkaStreams conversationEventsStream() {
        ConsumerService consumerService = new ConsumerService(postBoxRepository, shortTenantName);
        KafkaStreamProvider streamProvider = new KafkaStreamProvider(consumerService, kafkaServers,
                KafkaTopicService.CONVERSATION_EVENTS_KAFKA_TOPIC, shortTenantName);
        return streamProvider.provide();
    }
}
