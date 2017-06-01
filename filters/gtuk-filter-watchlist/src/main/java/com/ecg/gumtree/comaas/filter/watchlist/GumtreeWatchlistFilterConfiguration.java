package com.ecg.gumtree.comaas.filter.watchlist;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeWatchlistFilterConfiguration.WatchlistFilterFactory.class)
public class GumtreeWatchlistFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(GumshieldApi gumshieldApi) {
        return new WatchlistFilterFactory(gumshieldApi);
    }

    static class WatchlistFilterFactory extends GumtreeFilterFactory<WatchlistFilterConfig, GumtreeWatchlistFilter> {
        WatchlistFilterFactory(GumshieldApi gumshieldApi) {
            super(WatchlistFilterConfig.class,
                    (a, b) -> new GumtreeWatchlistFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b)
                            .withChecklistApi(gumshieldApi.checklistApi()));
        }
    }
}
