package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.google.common.base.Splitter;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.BindTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.annotation.PreDestroy;
import java.util.Iterator;

/**
 * @author alindhorst (original author)
 */


@Profile(ReplyTS.PRODUCTIVE_PROFILE)
public class ElasticSearchClientConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchClientConfiguration.class);


    private final TransportClient esClient;

    @Autowired
    ElasticSearchClientConfiguration(@Value("${search.es.endpoints}") String endpoints, @Value("${search.es.clustername}") String clusterName) {
        LOG.info("Productive ES Node configuration. endpoints: {}, clustername: {}", endpoints, clusterName);
        try {
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("cluster.name", clusterName).build();
            esClient = new TransportClient(settings);
            for (String endpoint : Splitter.on(',').trimResults().omitEmptyStrings().split(endpoints)) {
                Iterator<String> host = Splitter.on(':').trimResults().split(endpoint).iterator();
                InetSocketTransportAddress transportAddress = new InetSocketTransportAddress(host.next(), Integer.valueOf(host.next()));
                LOG.info("Known ES endpoint: {}", endpoint);
                esClient.addTransportAddress(transportAddress);
            }

        } catch (BindTransportException e) {
            LOG.error("Could not launch local ES client", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Client esClient() {
        return esClient;
    }

    @PreDestroy
    void stopEsNode() {
        esClient.close();
    }
}
