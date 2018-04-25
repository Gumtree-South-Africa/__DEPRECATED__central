package com.ecg.comaas.gtau.listener.messageevent;

import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Configuration
@Import({RTSRMQEventCreator.class, MessageEventListener.class})
public class MessageEventConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventConfiguration.class);

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.port}")
    private Integer port;

    @Value("${rabbitmq.virtualHost}")
    private String virtualHost;

    @Value("${rmq-msg-event-producer.username:${rabbitmq.username}}")
    private String username;

    @Value("${rmq-msg-event-producer.password:${rabbitmq.password}}")
    private String password;

    @Value("${rmq-msg-event-producer.connectionTimeout:1000}")
    private Integer connectionTimeout;

    @Value("${rmq-msg-event-producer.endpoint:gt.topic.default}")
    private String endpoint;

    @Bean(name = "rabbitMQConfigProducer")
    public RabbitMQConfiguration getRabbitMQConfigProducer() {
        LOG.info("Creating configuration with host={}, port={}, endpoint={}", host, port, endpoint);

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
}
