package com.ecg.replyts.core.runtime.configadmin;

import ecg.unicom.events.configuration.Configuration;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

import static java.lang.String.format;

public class ConfigurationConsumer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationConsumer.class);

    private final String kafkaEndpoint;
    private final String kafkaTopic;
    private final String allowedTenant;
    private final ConfigurationService configurationService;
    private KafkaStreams kafkaStreams;

    ConfigurationConsumer(ConfigurationService configurationService, String kafkaEndpoint, String kafkaTopic, String allowedTenant) {
        this.configurationService = configurationService;
        this.kafkaEndpoint = kafkaEndpoint;
        this.kafkaTopic = kafkaTopic;
        this.allowedTenant = allowedTenant;
    }

    void start() {
        LOG.info("Initializing '{}' with Endpoint: '{}', Topic: '{}'",
                ConfigurationConsumer.class.getName(), kafkaEndpoint, kafkaTopic);

        Properties consumerProperties = createConsumerProperties(kafkaEndpoint);
        Topology topology = createTopology(configurationService, builder -> builder.stream(kafkaTopic));
        this.kafkaStreams = new KafkaStreams(topology, consumerProperties);
        this.kafkaStreams.start();
    }

    public void close() {
        if (kafkaStreams != null) {
            kafkaStreams.close();
        }
    }

    private Topology createTopology(ConfigurationService consumerService, Function<StreamsBuilder, KStream<String, byte[]>> streamInit) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, byte[]> events = streamInit.apply(builder);
        // Filter out configurations belonging to different tenants
        events.filter((k, e) -> allowedTenant.equals(k))
                .mapValues(ConfigurationConsumer::decodeMessage)
                .filter((k, e) -> e != null)
                .foreach((k, e) -> consumerService.processEvent(e));
        return builder.build();
    }

    private static Configuration.Envelope decodeMessage(byte[] event) {
        try {
            return Configuration.Envelope.parseFrom(event);
        } catch (IOException e) {
            LOG.error("Exception during decoding a message", e);
            return null;
        }
    }

    private static Properties createConsumerProperties(String kafkaEndpoint) {
        final Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaEndpoint);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ComaasConfigurationConsumerGroup");
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
