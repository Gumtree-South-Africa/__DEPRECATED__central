package com.ecg.replyts.integration.riak;


import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.ecg.replyts.core.runtime.ReplyTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Profile(ReplyTS.EMBEDDED_PROFILE)
public class EmbeddedRiakClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedRiakClientConfiguration.class);

    private IRiakClient riakClient;
    private static List<IRiakClient> runningclients = new ArrayList();

    @PostConstruct
    void setup() throws RiakException {
        LOG.info("initializing mock riak client");
        riakClient = new EmbeddedRiakClient();
        runningclients.add(riakClient);
    }


    @Bean
    public IRiakClient riakClient() {
        return riakClient;
    }

    @PreDestroy
    void shutdown() {
        riakClient.shutdown();
    }

    public static void resetBrain(){
        for (IRiakClient runningclient : runningclients) {
            runningclient.shutdown();
        }

    }

}
