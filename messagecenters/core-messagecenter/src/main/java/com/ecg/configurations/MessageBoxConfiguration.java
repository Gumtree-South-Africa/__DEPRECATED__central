package com.ecg.configurations;

import com.ecg.messagebox.consumer.ConsumerConfiguration;
import com.ecg.replyts.core.api.model.Tenants;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@ComaasPlugin
@Configuration
@Profile({Tenants.TENANT_MDE, Tenants.TENANT_MP, Tenants.TENANT_MVCA})
@ComponentScan(value = "com.ecg.messagebox", excludeFilters =
@ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagebox.resources.*"))
@Import(ConsumerConfiguration.class)
public class MessageBoxConfiguration {

    @Bean
    public SpringContextProvider newV2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, context, "com.ecg.messagebox.resources");
    }
}
