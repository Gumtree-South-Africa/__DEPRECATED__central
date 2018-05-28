package com.ecg.replyts.app.search.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchClientConfiguration {

    @Bean(destroyMethod = "close")
    public RestClient elasticRestClient(@Value("${search.es.endpoints:localhost}") String endpoint) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("comaas", "DAWjeY46AAR48wEK"));

        return RestClient.builder(new HttpHost(endpoint, 443, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
    }

    @Bean
    public ElasticSearchSearchService elasticService(
            @Value("${search.es.indexname:replyts}") String indexName,
            RestClient client,
            ElasticDeleteClient deleteClient) {

        return new ElasticSearchSearchService(new RestHighLevelClient(client), deleteClient, indexName);
    }

    @Bean(destroyMethod = "close")
    public ElasticDeleteClient deleteByQueryClient(
            @Value("${search.es.endpoints:localhost}") String endpoint,
            @Value("${search.es.indexname:replyts}") String indexName) {

        return new ElasticDeleteClient(endpoint, indexName);
    }
}