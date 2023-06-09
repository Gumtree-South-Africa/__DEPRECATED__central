package com.ecg.comaas.gtuk.listener.eventpublisher.config;

import com.ecg.comaas.gtuk.listener.eventpublisher.MessageReceivedListener;
import com.ecg.comaas.gtuk.listener.eventpublisher.publisher.EventPublisher;
import com.ecg.comaas.gtuk.listener.eventpublisher.publisher.RabbitEventPublisher;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@Configuration
@Profile(TENANT_GTUK)
@ComaasPlugin
public class EventPublisherConfig {

    @Value("${gumtree.rabbitmq.host:localhost}")
    private String rabbitMqHost;

    @Value("${gumtree.rabbitmq.port:5672}")
    private int rabbitMqPort;

    @Value("${gumtree.rabbitmq.exchange.messaging:messaging}")
    private String exchangeName;

    @Value("${gumtree.rabbitmq.username.replyts2:replyts2}")
    private String username;

    @Value("${gumtree.rabbitmq.password.replyts2:replyts2}")
    private String password;

    @Value("${gumtree.rabbitmq.virtualhost.messaging:/messaging}")
    private String vhost;

    @Value("${gumtree.rabbitmq.message.sent.routingkey:}")
    private String messageSentRoutingKey;

    @Value("${gumtree.replyts2.message.publisher.enabled:false}")
    private boolean publisherEnabled;

    @Bean
    public EventPublisher rabbitEventPublisher(){
        Assert.hasLength(exchangeName);

        return new RabbitEventPublisher(connectionFactory(), exchangeName, messageSentRoutingKey);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        Assert.hasText(rabbitMqHost);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMqHost);
        connectionFactory.setPort(rabbitMqPort);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(vhost);
        return connectionFactory;
    }

    @Bean
    public MessageReceivedListener messageReceivedListener() {
        return new MessageReceivedListener(rabbitEventPublisher(), publisherEnabled);
    }
}
