package com.ecg.replyts2.eventpublisher.rabbitmq;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.app.eventpublisher.MessageReceivedListener;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Conditionally configure event publishing to Rabbit MQ.
 *
 * Enable publishing by adding the following to the {@code replyts.properties}:
 *
 * <pre>{@code
 * replyts.event.publisher.rabbitmq.enabled = true
 * ... more rabbitmq settings ...
 * }
 * </pre>
 */
@Configuration
public class RabbitmqEventPublisherConfig {

    @Value("${replyts.event.publisher.rabbitmq.enabled:false}")
    private boolean rabbitmqEnabled;
    @Value("${replyts.rabbitmq.host:localhost}")
    private String rabbitMqHost;
    @Value("${replyts.rabbitmq.port:5672}")
    private int rabbitMqPort;
    @Value("${replyts.rabbitmq.exchange:communication}")
    private String exchangeName;
    @Value("${replyts.rabbitmq.username:replyts2}")
    private String username;
    @Value("${replyts.rabbitmq.password:replyts2}")
    private String password;
    @Value("${replyts.rabbitmq.virtualhost:/conversations}")
    private String vhost;
    @Value("${replyts.rabbitmq.message.sent.routingkey:}")
    private String messageSentRoutingKey;

    @Autowired
    private MailCloakingService mailCloakingService;

    private Connection connection;
    private Channel channel;

    @Bean
    @Conditional(RabbitMqEnabledConditional.class)
    public MessageReceivedListener rabbitMqMessageReceivedListener() throws Exception {
        return new MessageReceivedListener(mailCloakingService, newRabbitEventPublisher());
    }

    private EventPublisher newRabbitEventPublisher() throws Exception {
        Assert.hasLength(exchangeName);
        Assert.hasText(rabbitMqHost);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMqHost);
        connectionFactory.setPort(rabbitMqPort);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(vhost);

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        return new RabbitEventPublisher(channel, exchangeName, messageSentRoutingKey);
    }

    @PreDestroy
    private void cleanup() throws IOException, TimeoutException {
        if (channel != null) channel.close();
        channel = null;
        if (connection != null) connection.close();
        connection = null;
    }

    public static class RabbitMqEnabledConditional implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Boolean.parseBoolean(context.getEnvironment().getProperty("replyts.event.publisher.rabbitmq.enabled", "false"));
        }
    }
}
