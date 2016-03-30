package com.ecg.replyts.integration.elasticsearch;

import com.ecg.replyts.core.runtime.ReplyTS;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile(ReplyTS.EMBEDDED_PROFILE)
public class EmbeddedElasticSearchClientConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedElasticSearchClientConfiguration.class);

    @Value("${search.es.endpoints}")
    private String endpoints;
    @Value("${search.es.clustername}")
    private String clusterName;

    private Node node;

    @Bean
    public Client esClient() {
        EnvironmentBasedNodeConfigurator nodeConfigurator = new EnvironmentBasedNodeConfigurator(clusterName, endpoints);

        node = nodeConfigurator.forEmbeddedEnvironments();

        return node.client();
    }

    // We never destroy the clients as this incurs concurrency issues with ElasticSearch 1.x
}