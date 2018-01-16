package com.ecg.messagecenter.listeners;

import com.ecg.messagecenter.pushmessage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PushServiceConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PushServiceConfiguration.class);

    private static final String PUSH_SERVICE_INFOPOOL_HOST = "app.gumtree.sg";

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "true")
    private PushService pushMobilePushService(@Value("${push-mobile.host}") String host, @Value("${push-mobile.port}") int port) {
        LOG.info("Kmobile push service enabled. Host: {} / Port: {}", host, port);

        return new KmobilePushService(host, port);
    }

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "true")
    private AdImageLookup pushMobileAdImageLookup(@Value("${push-mobile.host}") String host, @Value("${push-mobile.port}") int port) {
        return new KmobileAdImageLookup(host, port);
    }

    @Bean
    @ConditionalOnProperty(value = "mdns.enabled", havingValue = "true")
    private PushService mdnsPushService(@Value("${mdns.host}") String host, @Value("${mdns.authHeader}") String authHeader, @Value("${mdns.provider}") String provider) {
        LOG.info("MDNS push service enabled. Host: {} / Auth Header: {} / Provider: {}", host, authHeader, provider);

        return new MdsPushService(host, authHeader, provider);
    }

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "false", matchIfMissing = true)
    private AdImageLookup mdsAdImageLookup() {
        return new MdsAdImageLookup();
    }

    @Bean
    @ConditionalOnProperty(value = "pushservice.enabled", havingValue = "true")
    private PushService pushServicePushService(@Value("${pushservice.host:#{null}}") String host, @Value("${pushservice.port:#{null}}") int port) {
        LOG.info("Bolt push service enabled. Host: {} / Port: {}", host, port);

        return new BoltPushService(host, port);
    }

    @Bean
    @ConditionalOnExpression("#{'${push-mobile.enabled:false}' == 'false' && '${mdns.enabled:false}' == 'false' && '${pushservice.enabled:false}' == 'false'}")
    private PushService infoPoolPushService() {
        LOG.info("Info pool push service enabled. Host: {}", PUSH_SERVICE_INFOPOOL_HOST);

        return new InfoPoolPushService(PUSH_SERVICE_INFOPOOL_HOST);
    }
}