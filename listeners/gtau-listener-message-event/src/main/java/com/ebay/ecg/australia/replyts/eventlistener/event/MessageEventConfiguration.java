package com.ebay.ecg.australia.replyts.eventlistener.event;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Created by fmiri on 24/03/2017.
 */
public class MessageEventConfiguration {

    @Value("${rabbitmq-producer.host}")
    private String host;

    @Value("${rabbitmq-producer.port}")
    private Integer port;

    @Value("${rabbitmq-producer.username}")
    private String username;

    @Value("${rabbitmq-producer.password}")
    private String password;

    @Value("${rabbitmq-producer.connectionTimeout}")
    private Integer connectionTimeout;

    @Value("${rabbitmq-producer.virtualHost}")
    private String virtualHost;

    @Value("${rabbitmq-producer.endpoint}")
    private String endpoint;

    /* Start Producer Configuration */

    @Bean(name = "rabbitMQConfigProducer")
    public RabbitMQConfiguration getRabbitMQConfigProducer() {
        System.out.println("Creating configuration with these parameters:");
        System.out.println("[host=" + host + ",port="  + port + ",endpoint=" + endpoint);

        final RabbitMQConfiguration config = new RabbitMQConfiguration();
        config.setHost(host);
        config.setPort(port);
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(connectionTimeout);
        config.setVirtualHost(virtualHost);
        config.setEndpoint(endpoint);
        return config;
    }

    @Bean(destroyMethod = "close", name = "rabbitMQProducerEventHandlerClient")
    public RabbitMQEventHandlerClient getRabbitMQHandlerClient(@Qualifier("rabbitMQConfigProducer") RabbitMQConfiguration config)
            throws KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException, IOException, TimeoutException {
        return new RabbitMQEventHandlerClient(config);
    }
    /* End Producer Configuration */
}
