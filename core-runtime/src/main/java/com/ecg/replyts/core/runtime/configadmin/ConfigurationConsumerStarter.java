package com.ecg.replyts.core.runtime.configadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

public class ConfigurationConsumerStarter implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationConsumerStarter.class);

    private final ConfigurationConsumer consumer;

    public ConfigurationConsumerStarter(ConfigurationConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        consumer.start();
        LOG.info("Configuration consumer has been started");
    }
}
