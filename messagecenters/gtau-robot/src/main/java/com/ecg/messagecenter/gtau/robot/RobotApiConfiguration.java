package com.ecg.messagecenter.gtau.robot;

import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConsumerConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerConsumer;
import com.ecg.messagecenter.gtau.robot.handler.RabbitMQConsumer;
import com.ecg.messagecenter.gtau.robot.service.RobotService;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Configuration
@Import(RobotService.class)
@Profile(TENANT_GTAU)
public class RobotApiConfiguration {

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.port}")
    private Integer port;

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.connectionTimeout}")
    private Integer connectionTimeout;

    @Value("${rabbitmq.virtualHost}")
    private String virtualHost;

    @Value("${rabbitmq.endpoint}")
    private String endpoint;

    @Bean
    public SpringContextProvider robotContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/gtau-robot", RobotWebConfiguration.class, context);
    }

    @Bean(name = "rabbitMQConfigConsumer")
    public RabbitMQConfiguration getRabbitMQConfiguration() {
        RabbitMQConfiguration rabbitMQConfiguration = new RabbitMQConfiguration();
        rabbitMQConfiguration.setHost(host);
        rabbitMQConfiguration.setPort(port);
        rabbitMQConfiguration.setUsername(username);
        rabbitMQConfiguration.setPassword(password);
        rabbitMQConfiguration.setConnectionTimeout(connectionTimeout);
        rabbitMQConfiguration.setVirtualHost(virtualHost);
        rabbitMQConfiguration.setEndpoint(endpoint);
        return rabbitMQConfiguration;
    }

    @Bean
    public RabbitMQConsumer getRabbitMQConsumer(RobotService robotService) {
        return new RabbitMQConsumer(robotService);
    }

    @Bean(name = "gtau-robot-event-handler")
    public RabbitMQConsumerConfiguration getRabbitMQConfig(RabbitMQConsumer rabbitMQConsumer) {
        RabbitMQConsumerConfiguration config = new RabbitMQConsumerConfiguration();
        config.setEventHandler(rabbitMQConsumer);
        return config;
    }

    @Bean(destroyMethod = "close")
    public RabbitMQEventHandlerConsumer getRabbitMQEventHandlerConsumer(
            @Qualifier("rabbitMQConfigConsumer") RabbitMQConfiguration config, RabbitMQConsumerConfiguration consumerConfiguration
    ) throws KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException, IOException {
        return new RabbitMQEventHandlerConsumer(config, consumerConfiguration);
    }
}
