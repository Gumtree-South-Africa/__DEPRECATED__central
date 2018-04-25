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
@ConditionalOnExpression("#{'${replyts.tenant}' == 'mde' || '${replyts.tenant}' == 'mp'}")
@ComponentScan(value = "com.ecg.messagebox", excludeFilters =
@ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*"}))
public class MessageBoxConfiguration {

    @Bean
    public SpringContextProvider v2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgcenter", WebConfiguration.class, context, "com.ecg.messagebox.controllers");
    }

    @Bean
    public SpringContextProvider newV2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, context, "com.ecg.messagebox.resources");
    }
}
