package com.ecg.configurations;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@ComaasPlugin
@Configuration
@ConditionalOnProperty(name = "replyts.tenant", havingValue = "ebayk")
@ComponentScan(value = "com.ecg.messagecenter", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.ebayk.webapi.*"))
public class EbaykMessageBoxConfiguration {

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/ebayk-msgcenter", WebConfiguration.class, context, "com.ecg.messagecenter.ebayk.webapi");
    }
}
