package com.ecg.messagebox.consumer;

import com.ecg.comaas.events.Conversation.Envelope;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import static java.lang.String.format;

class KafkaStreamProvider {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamProvider.class);

    private final ConsumerService consumerService;
    private final String tenantShort;
    private final String kafkaEndpoints;
    private final String kafkaTopic;

    KafkaStreamProvider(ConsumerService consumerService, String kafkaEndpoints, String kafkaTopic, String tenantShort) {
        this.consumerService = consumerService;
        this.tenantShort = Objects.requireNonNull(tenantShort, "tenantShort must not be null");
        this.kafkaEndpoints = kafkaEndpoints;
        this.kafkaTopic = kafkaTopic;
    }

    KafkaStreams provide() {
        LOG.info("Initializing '{}' with Endpoint: '{}', Topic: '{}'",
                KafkaStreamProvider.class.getName(), kafkaEndpoints, kafkaTopic);

        Properties consumerProperties = createConsumerProperties(kafkaEndpoints, tenantShort);
        Topology topology = createTopology(consumerService, builder -> builder.stream(kafkaTopic));
        return new KafkaStreams(topology, consumerProperties);
    }

    private Topology createTopology(ConsumerService consumerService, Function<StreamsBuilder, KStream<String, byte[]>> streamInit) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, byte[]> events = streamInit.apply(builder);
        events.mapValues(KafkaStreamProvider::decodeMessage)
                .filter((k, e) -> e != null)
                .filter((k, e) -> tenantShort.equals(e.getTenant()))
                .foreach((k, e) -> consumerService.processEvent(e));
        return builder.build();
    }

    private static Envelope decodeMessage(byte[] event) {
        final Envelope envelope;
        try {
            envelope = Envelope.parseFrom(event);
        } catch (IOException e) {
            LOG.error("decodeMessage: ", e);
            return null;
        }
        return envelope;
    }

    private static Properties createConsumerProperties(String kafkaEndpoint, String tenantShort) {
        final Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaEndpoint);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ComaasLegacyMessageboxConsumerGroup_" + tenantShort);
        String allocId = System.getenv("NOMAD_ALLOC_ID");
        if (allocId != null) {
            long threadId = Thread.currentThread().getId();
            props.put(StreamsConfig.CLIENT_ID_CONFIG, format("%s-%d", allocId, threadId));
        }
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
        return props;
    }
}
