package com.ecg.comaas.r2cmigration.difftool;

import com.ecg.de.kleinanzeigen.replyts.graphite.GraphiteExporter;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.core.runtime.persistence.RiakHostConfig;
import com.ecg.replyts.core.runtime.persistence.strategy.CassandraPersistenceConfiguration;
import com.ecg.replyts.core.runtime.persistence.strategy.RiakPersistenceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.net.UnknownHostException;

@Configuration
@ComponentScan("com.ecg.comaas.r2cmigration.difftool")
@PropertySource("file:${confDir}/difftool.properties")
@Import({
        RiakHostConfig.class,
        RiakPersistenceConfiguration.RiakClientConfiguration.class,
        CassandraPersistenceConfiguration.CassandraClientConfiguration.class,
        JacksonAwareObjectMapperConfigurer.class
})
public class DiffToolConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(DiffToolConfiguration.class);

    static final String RIAK_CONVERSATION_BUCKET_NAME = "conversation";

    static final String RIAK_SECONDARY_INDEX_MODIFIED_AT = "modifiedAt";

    @Value("${graphite.enabled:true}") boolean isEnabled;
    @Value("${graphite.endpoint.hostname:graph001}") String hostname;
    @Value("${graphite.endpoint.port:2003}") int port;
    @Value("${graphite.timeperiod.sec:10}") int timeout;
    @Value("${graphite.prefix:difftool}") String prefix;

    @Bean
    GraphiteExporter graphiteExporter() {
        try {
            return new GraphiteExporter(isEnabled, hostname, port, timeout, prefix);
        } catch (UnknownHostException he) {
            LOG.error("Failed to connect to Graphite with : " + he.getLocalizedMessage(), he);
        }
        return null;
    }

}
