package com.ecg.replyts.app.search.elasticsearch;

import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author alindhorst (original author)
 */
public class ElasticSearchServiceConfiguration {

    @Value("${search.es.indexname:replyts}")
    private String indexName="replyts";

    @Value("${search.es.timeout.ms:20000}")
    private long timeoutMs;
    
    @Bean
    @Autowired
    public ElasticSearchSearchService searchService(Client client) {
        return new ElasticSearchSearchService(client, indexName, timeoutMs);
    }

}
