package com.ecg.replyts.core.webapi.control.health;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.runtime.maildelivery.smtp.SmtpMailDeliveryCheck;
import com.ecwid.consul.v1.ConsulClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Configuration
public class HealthConfiguration {

    @Value("${mailpublisher.kafka.broker.list:localhost:9092}")
    private String kafkaBrokers;

    @Autowired(required = false)
    private Map<String, DataSource> dataSources;

    @Autowired(required = false)
    private Map<String, Session> sessions;

    @Autowired(required = false)
    private Map<String, ConsistencyLevel> consistencyLevels;

    @Autowired(required = false)
    private ConsulClient consulClient;

    @Autowired(required = false)
    private Client elasticClient;

    @Autowired(required = false)
    private SmtpMailDeliveryCheck smtpMailDeliveryCheck;

    @Bean
    public HealthCommand cassandraHealthCommand() {
        if (sessions == null || consistencyLevels == null) {
            return new NoOpHealthCommand(CassandraHealthCommand.COMMAND_NAME);
        } else {
            return new CassandraHealthCommand(sessions, consistencyLevels);
        }
    }

    @Bean
    public HealthCommand elasticsearchHealthCommand() {
        if (elasticClient == null) {
            return new NoOpHealthCommand(ElasticsearchHealthCommand.COMMAND_NAME);
        } else {
            return new ElasticsearchHealthCommand(elasticClient);
        }
    }

    @Bean
    public HealthCommand mailHealthCommand() {
        if (smtpMailDeliveryCheck == null) {
            return new NoOpHealthCommand(MailHealthCommand.COMMAND_NAME);
        } else {
            return new MailHealthCommand(smtpMailDeliveryCheck);
        }
    }

    @Bean
    public HealthCommand datasourceHealthCheck() {
        if (dataSources == null) {
            return new NoOpHealthCommand(DataSourceHealthCommand.COMMAND_NAME);
        } else {
            return new DataSourceHealthCommand(dataSources);
        }
    }

    @Bean
    public HealthCommand consulHealthCommand() {
        if (consulClient == null) {
            return new NoOpHealthCommand(DataSourceHealthCommand.COMMAND_NAME);
        } else {
            return new ConsulHealthCommand(consulClient);
        }
    }

    @Bean
    public ConfigurationHealthCommand configurationHealthCommand() {
        return new ConfigurationHealthCommand(elasticClient);
    }

    @Bean
    public HealthCommand kafkaHealthCommand() {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        return new KafkaHealthCommand(new KafkaConsumer<>(props));
    }
}