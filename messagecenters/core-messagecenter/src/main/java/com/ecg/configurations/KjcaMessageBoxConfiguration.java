package com.ecg.configurations;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@ComaasPlugin
@Configuration
@ConditionalOnExpression("'${replyts.tenant}' == 'kjca'")
@ComponentScan(value = {"com.ecg.messagebox", "com.ecg.sync"}, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*"}))
@ComponentScan(value = "com.ecg.messagecenter", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.kjca.webapi.*"))
public class KjcaMessageBoxConfiguration {

    @Bean
    public SpringContextProvider v2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, context, "com.ecg.messagebox.resources", "com.ecg.sync");
    }

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/message-center", WebConfiguration.class, context, "com.ecg.messagecenter.kjca.webapi");
    }
}
