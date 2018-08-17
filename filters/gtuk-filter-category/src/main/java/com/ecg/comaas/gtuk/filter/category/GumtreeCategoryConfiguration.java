package com.ecg.comaas.gtuk.filter.category;

import com.gumtree.api.category.CategoryMapperFactory;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.CategoryReadApi;
import com.gumtree.api.category.CategoryReadApiClient;
import com.gumtree.api.config.CategoryModelFactory;
import com.gumtree.common.util.http.HttpClientFactory;
import com.gumtree.common.util.http.JsonHttpClient;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GumtreeCategoryConfiguration {
    @Value("${gumtree.category.api.hostname:localhost}")
    private String hostname;

    @Value("${gumtree.category.api.port:9000}")
    private Integer port;

    @Value("${gumtree.category.api.socket.timeout:5000}")
    private Integer socketTimeout;

    @Value("${gumtree.category.api.connection.timeout:15000}")
    private Integer connectionTimeout;

    @Value("${gumtree.category.api.retry.count:3}")
    private Integer retryCount;

    @Value("${gumtree.category.api.cache.reload.interval:5000}")
    private Integer cacheCheckInterval;

    @Value("${gumtree.category.api.use.mock:false}")
    private Boolean useMock;

    @Bean
    public CategoryReadApi categoryReadApi() {
        return useMock
                ? new MockCategoryReadApi()
                : buildCategoryReadApi();
    }

    public CategoryReadApi buildCategoryReadApi() {
        ObjectMapper objectMapper = (new CategoryMapperFactory()).create();
        HttpClient httpClient = (new HttpClientFactory()).create(socketTimeout, connectionTimeout, retryCount);
        JsonHttpClient jsonHttpClient = new JsonHttpClient(httpClient, objectMapper);
        return new CategoryReadApiClient(hostname, port, jsonHttpClient);
    }

    @Bean
    public CategoryModel unfilteredCategoryModel(CategoryReadApi categoryReadApi) {
        return new CategoryModelFactory(categoryReadApi, cacheCheckInterval).createWithoutFilters();
    }
}
