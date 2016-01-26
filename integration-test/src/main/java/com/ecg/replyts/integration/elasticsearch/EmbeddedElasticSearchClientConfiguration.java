package com.ecg.replyts.integration.elasticsearch;

import com.ecg.replyts.core.runtime.ReplyTS;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.annotation.PreDestroy;

@Profile(ReplyTS.EMBEDDED_PROFILE)
public class EmbeddedElasticSearchClientConfiguration {

    @Value("${search.es.endpoints}")
    private String endpoints;
    @Value("${search.es.clustername}")
    private String clusterName;

    private Node node;

    private static volatile Client lastClient;

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedElasticSearchClientConfiguration.class);

    @Bean
    public Client esClient() {
        EnvironmentBasedNodeConfigurator nodeConfigurator = new EnvironmentBasedNodeConfigurator(clusterName, endpoints);
        node = nodeConfigurator.forEmbeddedEnvironments();
        if (lastClient != null) {
            LOG.warn("WARNING!!! Multiple ES clients were instanciated!");
        }
        lastClient = node.client();
        return lastClient;
    }

    public static Client lastClient() {
        return lastClient;
    }

    @PreDestroy
    void stopEsNode() {
        node.stop();
        node.close();
    }

}
