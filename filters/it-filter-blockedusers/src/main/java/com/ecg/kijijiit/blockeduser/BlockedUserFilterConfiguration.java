package com.ecg.kijijiit.blockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;

@ComaasPlugin
@Configuration
@ComponentScan("com.ecg.kijijiit.blockeduser")
public class BlockedUserFilterConfiguration {
    @Bean
    public FilterFactory filterFactory(UserStateService userStateService) {
        return new BlockedUserFilterConfiguration.BlockedUserFilterFactory(userStateService);
    }

    public static class BlockedUserFilterFactory implements FilterFactory {
        private UserStateService userStateService;

        public BlockedUserFilterFactory(UserStateService userStateService) {
            this.userStateService = userStateService;
        }

        @Nonnull
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            return new BlockedUserFilter(userStateService);
        }
    }
}
