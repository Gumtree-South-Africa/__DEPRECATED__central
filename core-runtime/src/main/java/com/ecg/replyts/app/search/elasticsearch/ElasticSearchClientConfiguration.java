package com.ecg.replyts.app.search.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ElasticSearchClientConfiguration {

    @Bean(destroyMethod = "close")
    public RestClient elasticRestClient(
            @Value("${search.es.api.endpoint:http://localhost:9250}") String endpoint,
            @Value("#{systemEnvironment['ESAAS_USERNAME'] ?: 'not_used'}") String username,
            @Value("#{systemEnvironment['ESAAS_PASSWORD'] ?: 'not_used'}") String password
    ) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        URI uri = URI.create(endpoint);
        return RestClient.builder(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
    }

    @Bean
    public ElasticSearchService elasticService(
            @Value("${search.es.indexname:replyts}") String indexName,
            RestClient client,
            ElasticDeleteClient deleteClient) {

        return new ElasticSearchService(new RestHighLevelClient(client), deleteClient, indexName);
    }

    @Bean(destroyMethod = "close")
    public ElasticDeleteClient deleteByQueryClient(
            @Value("${search.es.endpoints:localhost}") String endpoint,
            @Value("${search.es.indexname:replyts}") String indexName) {

        return new ElasticDeleteClient(endpoint, indexName);
    }

}