package com.ecg.gumtree.comaas.filter.blacklist;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeBlacklistFilterConfiguration.BlacklistFilterFactory.class)
public class GumtreeBlacklistFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(GumshieldApi gumshieldApi) {
        return new BlacklistFilterFactory(gumshieldApi);
    }

    static class BlacklistFilterFactory extends GumtreeFilterFactory<BlacklistFilterConfig, GumtreeBlacklistFilter> {
        BlacklistFilterFactory(GumshieldApi gumshieldApi) {
            super(BlacklistFilterConfig.class,
                    (a, b) -> new GumtreeBlacklistFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b)
                            .withGumshieldApi(gumshieldApi));
        }
    }
}
