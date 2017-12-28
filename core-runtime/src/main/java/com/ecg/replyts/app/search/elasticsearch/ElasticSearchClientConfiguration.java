package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.Application;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.BindTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;

@Configuration
@Profile(Application.PRODUCTIVE_PROFILE)
public class ElasticSearchClientConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchClientConfiguration.class);

    private Client client;
    private Client xDCClient;

    @Bean(name = "esclient")
    public Client esClient(@Value("${search.es.endpoints}") String endpoints, @Value("${search.es.clustername}") String clusterName) {
        LOG.info("Productive ES Node configuration. endpoints: {}, clustername: {}", endpoints, clusterName);

        try {
            this.client = getTransportClient(endpoints, clusterName);
        } catch (BindTransportException e) {
            throw new RuntimeException("Could not launch local ES client", e);
        }
        return client;
    }


    private TransportClient getTransportClient(String endpoints, String clusterName) {
        TransportClient client = new TransportClient(ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build());

        for (String endpoint : StringUtils.tokenizeToStringArray(endpoints, ",", true, true)) {
            String[] hostPort = endpoint.split(":");

            if (hostPort.length != 2) {
                throw new IllegalArgumentException("Please supply the search.es.endpoints property as a comma-separated list of host:port values");
            }

            client.addTransportAddress(new InetSocketTransportAddress(hostPort[0], Integer.valueOf(hostPort[1])));
        }
        return client;
    }


    @Bean(name = "crossDCEsClient")
    @ConditionalOnExpression("#{ '${xdc.search.es.endpoints:}'.trim().length()>0 && '${xdc.search.es.clustername:}'.trim().length()>0 }")
    public Client crossDCClient(@Value("${xdc.search.es.endpoints}") String endpoints, @Value("${xdc.search.es.clustername}") String clusterName) {
        LOG.info("Productive cross-dc ES Node configuration. endpoints: {}, clustername: {}", endpoints, clusterName);
        try {
            this.xDCClient = getTransportClient(endpoints, clusterName);
        } catch (BindTransportException e) {
            throw new RuntimeException("Could not launch cross-DC ES client", e);
        }
        return xDCClient;
    }

    @PreDestroy
    void stopEsNode() {
        client.close();
        if (xDCClient != null) {
            xDCClient.close();
        }
    }
}