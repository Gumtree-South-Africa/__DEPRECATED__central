package com.ecg.configurations;

import com.ecg.replyts.core.api.model.Tenants;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@ComaasPlugin
@Configuration
@Profile({Tenants.TENANT_AR, Tenants.TENANT_SG, Tenants.TENANT_MX, Tenants.TENANT_ZA})
@ComponentScan(value = {"com.ecg.messagecenter.core", "com.ecg.messagecenter.bt"}, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.bt.webapi.*"))
public class BtMessageBoxConfiguration {

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/ebayk-msgcenter", WebConfiguration.class, context, "com.ecg.messagecenter.bt.webapi", "com.ecg.messagecenter.migration");
    }
}
