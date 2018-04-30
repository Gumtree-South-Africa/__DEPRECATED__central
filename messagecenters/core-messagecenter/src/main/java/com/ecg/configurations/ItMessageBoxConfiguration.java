package com.ecg.configurations;

import com.ecg.messagecenter.core.persistence.simple.CassandraSimplePostBoxRepository;
import com.ecg.replyts.core.api.model.Tenants;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

@ComaasPlugin
@Configuration
@Profile(Tenants.TENANT_IT)
@ComponentScan(value = { "com.ecg.messagecenter.core", "com.ecg.messagecenter.it" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.it.webapi.*"))
public class ItMessageBoxConfiguration {

    @Bean
    @Qualifier(CassandraSimplePostBoxRepository.THREAD_CONVERSATION_CLASS)
    public String conversationThreadClass() {
        return "com.ecg.messagecenter.it.persistence.ConversationThread";
    }

    @Bean
    public SpringContextProvider v1ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/ebayk-msgcenter", WebConfiguration.class, context, "com.ecg.messagecenter.it.webapi");
    }
}
