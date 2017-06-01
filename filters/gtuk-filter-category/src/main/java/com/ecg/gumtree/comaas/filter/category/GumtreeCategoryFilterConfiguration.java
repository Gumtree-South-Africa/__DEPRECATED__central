package com.ecg.gumtree.comaas.filter.category;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.CategoryReadApi;
import com.gumtree.api.config.CategoryModelFactory;
import com.gumtree.api.config.CategoryReadApiFactory;
import com.gumtree.filters.comaas.config.CategoryFilterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeCategoryFilterConfiguration.CategoryFilterFactory.class)
public class GumtreeCategoryFilterConfiguration {
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

    @Bean
    public FilterFactory filterFactory(CategoryModel categoryModel) {
        return new CategoryFilterFactory(categoryModel);
    }

    public static class CategoryFilterFactory extends GumtreeFilterFactory<CategoryFilterConfig, GumtreeCategoryFilter> {
        CategoryFilterFactory(CategoryModel categoryModel) {
            super(CategoryFilterConfig.class, (a, b) -> new GumtreeCategoryFilter().withCategoryModel(categoryModel));
        }
    }
}
