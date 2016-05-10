package com.ebay.ecg.replyts.robot;

import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConsumerConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerConsumer;
import com.ebay.ecg.replyts.robot.handler.RabbitMQConsumer;
import com.ebay.ecg.replyts.robot.service.RobotService;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author mdarapour
 */
@Configuration
class RobotApiConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

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

    @Autowired
    private EmbeddedWebserver webserver;

    @PostConstruct
    public void context() {
        webserver.context(new SpringContextProvider("/gtau-robot", new String[]{"classpath:gtau-robot-context.xml"}, applicationContext));
    }

    @Bean
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
            RabbitMQConfiguration config, RabbitMQConsumerConfiguration consumerConfiguration
    ) throws Exception {
        return new RabbitMQEventHandlerConsumer(config, consumerConfiguration);
    }
}
