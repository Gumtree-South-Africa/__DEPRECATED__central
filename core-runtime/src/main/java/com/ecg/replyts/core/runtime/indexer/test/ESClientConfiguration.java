package com.ecg.replyts.core.runtime.indexer.test;

import com.ecg.replyts.core.runtime.indexer.IndexDataBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/*
This configuration should NEVER be active in prod
 */
@Configuration
@ConditionalOnProperty(name = "doc2kafka.sink.enabled", havingValue = "false")
public class ESClientConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DirectESIndexer.class);

    public static final String ES_INDEX_NAME = "comaasidx";
    private static final String ES_HOST_NAME = "localhost";
    private static final int ES_HOST_PORT = 9300;
    private static final String ES_CLUSTER_NAME = "elasticsearch";

    @Bean
    public Client client() {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        Settings settings = Settings.builder().put("cluster.name", ES_CLUSTER_NAME).build();
        LOG.info("Connecting to ElasticSearch on '{}:{}' clustername '{}', index name '{}'", ES_HOST_NAME, ES_HOST_PORT, ES_CLUSTER_NAME, ES_INDEX_NAME);
        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(ES_HOST_NAME, ES_HOST_PORT)));
        return client;
    }

    @Bean
    public DirectESIndexer directESIndexer(Client client, IndexDataBuilder indexDataBuilder) {
        return new DirectESIndexer(ES_INDEX_NAME, client, indexDataBuilder);
    }

    @Bean
    public Document2ESSink document2ESSink(DirectESIndexer directESIndexer) {
        return new Document2ESSink(directESIndexer);
    }

}
