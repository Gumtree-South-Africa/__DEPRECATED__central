package com.ecg.replyts.integration.riak;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.ecg.replyts.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Profile(Application.EMBEDDED_PROFILE)
public class EmbeddedRiakClientConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedRiakClientConfiguration.class);

    private IRiakClient riakClient;

    @PostConstruct
    void setup() throws RiakException {
        LOG.info("initializing mock riak client");
        riakClient = new EmbeddedRiakClient();
    }

    @Bean
    public IRiakClient riakClient() {
        return riakClient;
    }

    @PreDestroy
    void shutdown() {
        riakClient.shutdown();
    }
}
