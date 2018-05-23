package com.ecg.replyts.app.search.elasticsearch;

import com.google.common.base.Splitter;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
public class ElasticSearchClientConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchClientConfiguration.class);

    private static final Consumer<String[]> ENDPOINT_CHECK = endpoint -> {
        if (endpoint.length != 2) {
            throw new IllegalArgumentException("Please supply the search.es.endpoints property as a comma-separated list of host:port values");
        }
    };

    @Bean
    public ElasticSearchSearchService elasticService(
            Client client,
            @Value("${search.es.timeout.ms:20000}") long timeout,
            @Value("${search.es.indexname:replyts}") String indexName) {

        ElasticQueryFactory queryFactory = new ElasticQueryFactory(client, indexName);
        return new ElasticSearchSearchService(queryFactory, TimeValue.timeValueMillis(timeout));
    }

    @Bean(destroyMethod = "close")
    public Client esClient(@Value("${search.es.endpoints}") String endpoints, @Value("${search.es.clustername}") String clusterName) {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        LOG.info("Productive ES Node configuration. endpoints: {}, clustername: {}", endpoints, clusterName);

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .build();

        TransportClient client = new PreBuiltTransportClient(settings);
        transformEndpoints(endpoints)
                .forEach(client::addTransportAddress);
        return client;
    }

    private static List<TransportAddress> transformEndpoints(String endpointsConcat) {
        List<String> endpoints = Splitter.on(",")
                .trimResults()
                .omitEmptyStrings()
                .splitToList(endpointsConcat);

        return endpoints.stream()
                .map(endpoint -> endpoint.split(":"))
                .peek(ENDPOINT_CHECK)
                .map(hostPort -> new InetSocketAddress(hostPort[0], Integer.valueOf(hostPort[1])))
                .map(InetSocketTransportAddress::new)
                .collect(Collectors.toList());
    }
}