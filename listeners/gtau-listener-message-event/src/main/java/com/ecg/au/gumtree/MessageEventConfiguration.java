package com.ecg.au.gumtree;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import com.ebay.ecg.australia.events.rabbitmq.RabbitMQConfiguration;
import com.ebay.ecg.australia.events.rabbitmq.RabbitMQEventHandlerClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by fmiri on 20/03/2017.
 *
 * @author Ima Miri <fmiri@ebay.com>
 */

@Configuration
public class MessageEventConfiguration {
    /* Start Producer Configuration */
    @Bean
    public RabbitMQProducerConfig getRabbitMQProducerConfig() {
        return new RabbitMQProducerConfig();
    }

    @ConfigurationProperties(prefix = "rabbitmq-producer")
    public static class RabbitMQProducerConfig extends RabbitMQConfiguration {
    }

    @Bean(destroyMethod = "close")
    public RabbitMQEventHandlerClient getRabbitMQHandlerClient(RabbitMQProducerConfig config)
            throws KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException, IOException, TimeoutException {
        return new RabbitMQEventHandlerClient(config);
    }
    /* End Producer Configuration */
}