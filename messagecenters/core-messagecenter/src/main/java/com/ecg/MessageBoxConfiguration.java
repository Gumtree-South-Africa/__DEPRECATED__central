package com.ecg;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@ComaasPlugin
@Configuration
@ConditionalOnExpression("#{'${replyts.tenant}' != 'kjca' && '${replyts.tenant}' != 'it' && '${replyts.tenant}' != 'ebayk'}")
public class MessageBoxConfiguration {

    static final String V2_AND_UPGRADE_TENANTS = "#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde' || ('${replyts.tenant}' == 'gtuk' && '${webapi.sync.uk.enabled}' == 'true') || ('${replyts.tenant}' == 'gtau' && '${webapi.sync.au.enabled}' == 'true')}";
    static final String ONLY_V1_TENANTS = "#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}";

    @Configuration
    @ComponentScan(value = {"com.ecg.messagebox", "com.ecg.sync"}, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*"}))
    @ConditionalOnExpression(V2_AND_UPGRADE_TENANTS)
    public static class MessageBoxServices {
        @Bean
        public SpringContextProvider v2ContextProvider(ApplicationContext context) {
            return new SpringContextProvider("/msgcenter", LegacyWebConfiguration.class, context);
        }
    }

    @Bean
    @ConditionalOnExpression(V2_AND_UPGRADE_TENANTS)
    public SpringContextProvider newV2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, context);
    }

    @Configuration
    @ComponentScan(value = "com.ecg.messagecenter", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.webapi.*"))
    @ConditionalOnExpression(ONLY_V1_TENANTS)
    public static class MessageCenterServices {
        @Bean
        public SpringContextProvider v1ContextProvider(@Value("${replyts.tenant}") String tenant, ApplicationContext context) {
            String tenantPath = "kjca".equals(tenant) ? "/message-center" : "/ebayk-msgcenter";

            return new SpringContextProvider(tenantPath, LegacyWebConfiguration.class, context);
        }
    }
}