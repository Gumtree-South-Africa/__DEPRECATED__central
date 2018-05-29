package com.ecg.replyts.app.search.elasticsearch;

import com.google.common.base.Splitter;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
public class ElasticSearchClientConfiguration {

    private static final Consumer<String[]> ENDPOINT_CHECK = endpoint -> {
        if (endpoint.length != 2) {
            throw new IllegalArgumentException("Please supply the search.es.endpoints property as a comma-separated list of host:port values");
        }
    };

    @Bean(destroyMethod = "close")
    public RestClient elasticRestClient(
            @Value("${search.es.api.endpoint:http://localhost:9250}") String endpoint,
            @Value("${search.es.api.username:#{systemEnvironment['ESAAS_USERNAME']}}") String username,
            @Value("${search.es.api.password:#{systemEnvironment['ESAAS_PASSWORD']}}") String password
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

    /**
     * ONLY FOR COMAAS INDEXER - DELETE WITH WHOLE FUNCTIONALITY
     */
    @Bean(destroyMethod = "close")
    public TransportClient client(
            @Value("${search.es.endpoints:localhost}") String endpoints,
            @Value("${search.es.clustername}") String clusterName) {

        System.setProperty("es.set.netty.runtime.available.processors", "false");
        Settings settings = Settings.builder()
                .put("cluster.name", clusterName).build();

        List<TransportAddress> esAddresses = transformEndpoints(endpoints);
        TransportClient client = new PreBuiltTransportClient(settings);
        esAddresses.forEach(client::addTransportAddress);
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