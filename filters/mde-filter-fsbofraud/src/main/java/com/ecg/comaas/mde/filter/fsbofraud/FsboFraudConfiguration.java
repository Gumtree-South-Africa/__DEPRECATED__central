package com.ecg.comaas.mde.filter.fsbofraud;

import com.ecg.comaas.mde.filter.fsbofraud.broker.RabbitMQClient;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class FsboFraudConfiguration {

    @Value("${replyts.mobile.fsbo.fraud.fsboCsWebserviceUrl}")
    private String webserviceUrl;

    @Value("${rabbitMQ.hosts}")
    private String rabbitMqHosts;

    @Bean
    public FsboFraudFilterFactory fsboFraudFilterFactory(FsboCsPlatformAdChecker checker, RabbitMQClient client) {
        return new FsboFraudFilterFactory(checker, client);
    }

    @Bean
    public FsboCsPlatformAdChecker fsboCsPlatformAdChecker() {
        return new FsboCsPlatformAdChecker(webserviceUrl);
    }

    @Bean
    public RabbitMQClient rabbitMQClient() {
        return new RabbitMQClient(rabbitMqHosts);
    }
}
