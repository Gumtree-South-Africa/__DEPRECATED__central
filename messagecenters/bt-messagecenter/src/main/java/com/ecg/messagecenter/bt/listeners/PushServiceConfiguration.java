package com.ecg.messagecenter.bt.listeners;

import com.ecg.messagecenter.bt.pushmessage.*;
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

    @Value("${infopool.host:}")
    private String infopoolHost;

    @Value("${infopool.proxy.host:}")
    private String proxyHost;

    @Value("${infopool.proxy.port:}")
    private Integer proxyPort;

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "true")
    public PushService pushMobilePushService(@Value("${push-mobile.host}") String host, @Value("${push-mobile.port}") int port) {
        LOG.info("Kmobile push service enabled. Host: {} / Port: {}", host, port);

        return new KmobilePushService(host, port);
    }

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "true")
    public AdImageLookup pushMobileAdImageLookup(@Value("${push-mobile.host}") String host, @Value("${push-mobile.port}") int port) {
        return new KmobileAdImageLookup(host, port);
    }

    @Bean
    @ConditionalOnProperty(value = "mdns.enabled", havingValue = "true")
    public PushService mdnsPushService(@Value("${mdns.host}") String host, @Value("${mdns.authHeader}") String authHeader, @Value("${mdns.provider}") String provider) {
        LOG.info("MDNS push service enabled. Host: {} / Auth Header: {} / Provider: {}", host, authHeader, provider);

        return new MdsPushService(host, authHeader, provider);
    }

    @Bean
    @ConditionalOnProperty(value = "push-mobile.enabled", havingValue = "false", matchIfMissing = true)
    public AdImageLookup mdsAdImageLookup() {
        return new MdsAdImageLookup();
    }

    @Bean
    @ConditionalOnProperty(value = "pushservice.enabled", havingValue = "true")
    public PushService pushServicePushService(@Value("${pushservice.host:#{null}}") String host, @Value("${pushservice.port:#{null}}") Integer port) {
        LOG.info("Bolt push service enabled. Host: {} / Port: {}", host, port);

        return new BoltPushService(host, port);
    }

    @Bean
    @ConditionalOnExpression("#{'${push-mobile.enabled:false}' == 'false' && '${mdns.enabled:false}' == 'false' && '${pushservice.enabled:false}' == 'false'}")
    public PushService infoPoolPushService() {
        LOG.info("Info pool push service enabled. Host: {}", infopoolHost);
        return new InfoPoolPushService(infopoolHost, proxyHost, proxyPort);
    }
}