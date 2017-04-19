package com.gumtree.comaas.filter.category;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.CategoryFilterConfig;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeCategoryFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return (instanceName, configuration) -> {
            JsonNode configurationNode = configuration.get("configuration");
            CategoryFilterConfig filterConfig = ConfigMapper.asObject(configurationNode.toString(), CategoryFilterConfig.class);
            return new GumtreeCategoryBreadcrumbFilter().withFilterConfig(filterConfig);
        };
    }
}
