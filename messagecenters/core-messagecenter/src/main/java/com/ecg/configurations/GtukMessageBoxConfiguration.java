package com.ecg.configurations;

import com.ecg.replyts.core.api.model.Tenants;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@ComaasPlugin
@Configuration
@Profile(Tenants.TENANT_GTUK)
@ComponentScan(value = {"com.ecg.messagecenter.core", "com.ecg.messagecenter.gtuk"}, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.gtuk.webapi.*"))
public class GtukMessageBoxConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "webapi.sync.uk.enabled", havingValue = "true")
    @ComponentScan(value = {"com.ecg.messagebox", "com.ecg.sync"}, excludeFilters =
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagebox.resources.*"))
    public static class MessageBoxConfiguration {
        @Bean
        public SpringContextProvider v2ContextProvider(ApplicationContext context) {
            return new SpringContextProvider("/msgbox", WebConfiguration.class, context, "com.ecg.messagebox.resources", "com.ecg.sync");
        }
    }

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/ebayk-msgcenter", WebConfiguration.class, context, "com.ecg.messagecenter.gtuk.webapi");
    }
}
