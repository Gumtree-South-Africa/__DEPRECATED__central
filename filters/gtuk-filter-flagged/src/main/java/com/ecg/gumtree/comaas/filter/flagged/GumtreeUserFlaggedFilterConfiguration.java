package com.ecg.gumtree.comaas.filter.flagged;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.UserFlaggedFilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeUserFlaggedFilterConfiguration.UserFlaggedFilterFactory.class)
public class GumtreeUserFlaggedFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new UserFlaggedFilterFactory();
    }

    static class UserFlaggedFilterFactory extends GumtreeFilterFactory<UserFlaggedFilterConfig, GumtreeUserFlaggedFilter> {
        UserFlaggedFilterFactory() {
            super(com.gumtree.filters.comaas.config.UserFlaggedFilterConfig.class,
                    (a, b) -> new com.ecg.gumtree.comaas.filter.flagged.GumtreeUserFlaggedFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b));
        }
    }
}
