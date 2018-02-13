package com.ecg;

import com.ecg.replyts.core.runtime.ComaasPlugin;
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
public class PluginConfiguration {
    protected static final String ONLY_V1_TENANTS = "#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}";
    protected static final String ONLY_V2_TENANTS = "#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde'}";
    protected static final String V2_AND_UPGRADE_TENANTS = "#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde' || ('${replyts.tenant}' == 'gtuk' && '${webapi.sync.uk.enabled}' == 'true') || ('${replyts.tenant}' == 'ebayk' && '${webapi.sync.ek.enabled}' == 'true')}";

    @Configuration
    @ComponentScan(value = "com.ecg.messagebox", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = { "com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*" }))
    @ConditionalOnExpression(V2_AND_UPGRADE_TENANTS)
    public static class MessageBoxServices {
        @Bean
        public SpringContextProvider v2ContextProvider(ApplicationContext context) {
            return new SpringContextProvider("/msgcenter", WebConfiguration.class, context);
        }
    }

    @Configuration
    @ComponentScan(value = "com.ecg.messagecenter", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.webapi.*"))
    @ConditionalOnExpression(ONLY_V1_TENANTS)
    public static class MessageCenterServices {
        @Bean
        public SpringContextProvider v1ContextProvider(@Value("${replyts.tenant}") String tenant, ApplicationContext context) {
            String tenantPath = "kjca".equals(tenant) ? "/message-center" : "/ebayk-msgcenter";

            return new SpringContextProvider(tenantPath, WebConfiguration.class, context);
        }
    }

    @Bean
    @ConditionalOnExpression(ONLY_V2_TENANTS)
    public SpringContextProvider newV2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, context);
    }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.migration")
    @ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
    public static class MigrationConfiguration {
    }
}
