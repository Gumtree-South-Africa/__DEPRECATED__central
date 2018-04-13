package com.ecg.comaas.gtuk.filter.category;

import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.CategoryReadApi;
import com.gumtree.api.config.CategoryModelFactory;
import com.gumtree.api.config.CategoryReadApiFactory;
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

    @Bean
    public CategoryReadApi categoryReadApi() {
        return new CategoryReadApiFactory(hostname, port, socketTimeout, connectionTimeout, retryCount).create();
    }

    @Bean
    public CategoryModel unfilteredCategoryModel(CategoryReadApi categoryReadApi) {
        return new CategoryModelFactory(categoryReadApi, cacheCheckInterval).createWithoutFilters();
    }
}
