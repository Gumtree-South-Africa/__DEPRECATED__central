package com.ecg.configurations;

import com.ecg.messagecenter.core.persistence.simple.CassandraSimplePostBoxRepository;
import com.ecg.replyts.core.api.model.Tenants;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

@ComaasPlugin
@Configuration
@Profile(Tenants.TENANT_GTAU)
@ComponentScan(value = { "com.ecg.messagecenter.core", "com.ecg.messagecenter.gtau" }, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.gtau.webapi.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.gtau.robot.*")})
public class GtauMessageBoxConfiguration {

    @Bean
    @Qualifier(CassandraSimplePostBoxRepository.THREAD_CONVERSATION_CLASS)
    public String conversationThreadClass() {
        return "com.ecg.messagecenter.gtau.persistence.ConversationThread";
    }

    @Configuration
    @ConditionalOnProperty(name = "webapi.sync.au.enabled", havingValue = "true")
    @ComponentScan(value = {"com.ecg.messagebox", "com.ecg.sync"}, excludeFilters =
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*"}))
    public static class MessageBoxConfiguration {
        @Bean
        public SpringContextProvider v2ContextProvider(ApplicationContext context) {
            return new SpringContextProvider("/msgbox", WebConfiguration.class, context, "com.ecg.messagebox.resources", "com.ecg.sync");
        }
    }

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/ebayk-msgcenter", WebConfiguration.class, context, "com.ecg.messagecenter.gtau.webapi");
    }
}
