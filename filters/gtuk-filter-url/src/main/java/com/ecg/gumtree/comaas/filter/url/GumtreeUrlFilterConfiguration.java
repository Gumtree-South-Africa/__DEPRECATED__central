package com.ecg.gumtree.comaas.filter.url;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.UrlFilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeUrlFilterConfiguration.UrlFilterFactory.class)
public class GumtreeUrlFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new UrlFilterFactory();
    }

    static class UrlFilterFactory extends GumtreeFilterFactory<UrlFilterConfig, GumtreeUrlFilter> {
        UrlFilterFactory() {
            super(UrlFilterConfig.class, (a, b) -> new GumtreeUrlFilter().withPluginConfig(a).withFilterConfig(b));
        }
    }
}
