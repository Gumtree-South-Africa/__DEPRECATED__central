package com.ecg.comaas.gtuk.filter.category;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GumtreeCategoryConfiguration {

    @Bean(destroyMethod = "close")
    public CategoryClient categoryClient (
            @Value("${gumtree.category.api.hostname:localhost}") String hostname,
            @Value("${gumtree.category.api.port:9000}") int port,
            @Value("${gumtree.category.api.socket.timeout:5000}") int socketTimeout,
            @Value("${gumtree.category.api.connection.timeout:15000}") int connectionTimeout,
            @Value("${gumtree.category.api.retry.count:3}") int retryCount) {

        return new DefaultCategoryClient(hostname, port, connectionTimeout, socketTimeout, retryCount);
    }

    @Bean
    public CategoryService defaultCategoryClient() {
        return DefaultCategoryService.createNewService();
    }

    @Bean(destroyMethod = "close")
    public CategoryChangeMonitor categoryChangeMonitor(
            @Value("${gumtree.category.api.cache.reload.interval:5000}") int checkInterval,
            CategoryClient categoryClient,
            CategoryService categoryService) {

        return new CategoryChangeMonitor(categoryClient, checkInterval, categoryService);
    }
}
