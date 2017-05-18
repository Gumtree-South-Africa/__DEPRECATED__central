package com.gumtree.comaas.filter.category;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.CategoryReadApi;
import com.gumtree.api.config.CategoryModelFactory;
import com.gumtree.api.config.CategoryReadApiFactory;
import com.gumtree.comaas.common.filter.DisabledFilter;
import com.gumtree.filters.comaas.config.CategoryFilterConfig;
import com.gumtree.filters.comaas.config.State;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeCategoryFilterConfiguration {
    @Value("${gumtree.category.api.hostname:localhost}")
    private String hostname;

    @Value("${gumtree.category.api.port:9000}")
    private Integer port;

    @Value("${gumtree.category.api.socket.timeout:5000}")
    private Integer socketTimeout;

    @Value("${gumtree.category.api.connection.timeout:15000}")
    private Integer connectionTimeout;

    @Value("${gumtree.category.api.retry.count:1}")
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
        return new CategoryBreadcrumbFilterFactory(categoryModel);
    }

    public static class CategoryBreadcrumbFilterFactory implements FilterFactory {
        private CategoryModel categoryModel;

        CategoryBreadcrumbFilterFactory(CategoryModel categoryModel) {
            this.categoryModel = categoryModel;
        }

        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            try {
                JsonNode configurationNode = configuration.get("configuration");
                CategoryFilterConfig filterConfig;
                filterConfig = new ObjectMapper().treeToValue(configurationNode, CategoryFilterConfig.class);

                if (filterConfig.getState() == State.DISABLED) {
                    return new DisabledFilter(this.getClass());
                }

                return new GumtreeCategoryBreadcrumbFilter().withCategoryModel(categoryModel);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not configure plugin GumtreeCategoryFilterConfiguration", e);
            }
        }
    }
}
