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
import org.elasticsearch.transport.BindTransportException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
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

    private TransportClient client;

    @Bean(destroyMethod = "close")
    public TransportClient client(String endpoints, String clusterName) {
        TransportClient client = this.client;
        if (client == null) {
            Settings settings = Settings.builder()
                    .put("cluster.name", clusterName).build();

            List<TransportAddress> esAddresses = transformEndpoints(endpoints);
            client = new PreBuiltTransportClient(settings);
            esAddresses.forEach(client::addTransportAddress);
            this.client = client;
        }
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