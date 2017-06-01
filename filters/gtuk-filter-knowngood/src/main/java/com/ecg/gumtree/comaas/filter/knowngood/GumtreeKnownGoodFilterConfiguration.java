package com.ecg.gumtree.comaas.filter.knowngood;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeKnownGoodFilterConfiguration.KnownGoodFilterFactory.class)
public class GumtreeKnownGoodFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(GumshieldApi gumshieldApi) {
        return new KnownGoodFilterFactory(gumshieldApi);
    }

    static class KnownGoodFilterFactory extends GumtreeFilterFactory<KnownGoodFilterConfig, GumtreeKnownGoodFilter> {
        KnownGoodFilterFactory(GumshieldApi gumshieldApi) {
            super(KnownGoodFilterConfig.class,
                    (a, b) -> new GumtreeKnownGoodFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b)
                            .withUserApi(gumshieldApi.userApi()));
        }
    }
}
