package com.ecg.replyts.gumtree;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.ecg.replyts.gumtree.healthcheck.WebConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration("gtuk-coremod-healthcheck")
public class PluginConfiguration {
    @Bean
    public SpringContextProvider contextProvider(ApplicationContext applicationContext) {
        return new SpringContextProvider("/internal", WebConfiguration.class, applicationContext);
    }
}