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

    private static final String ONLY_V1_TENANTS = "#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}";
    private static final String ONLY_V2_TENANTS = "#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde'}";
    private static final String V2_AND_UPGRADE_TENANTS = "#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde' || ('${replyts.tenant}' == 'gtuk' && '${webapi.sync.uk.enabled}' == 'true') || ('${replyts.tenant}' == 'ebayk' && '${webapi.sync.ek.enabled}' == 'true')}";

    /**
     * Scan and import only Spring Components without Controllers (Old MessageBox) and Resources (New MessageBox)
     * - Both sets of endpoints needs the same services (e.g. CassandraPostBoxService) as a parent spring application context and
     * in a child context only endpoints
     * - Otherwise we are not able to split it up into 2 DispatcherServlets to give them different URL contexts
     * - We cannot use one DispatcherServlet mapped "/*' and use context path in endpoint's annotation because we've already had
     * a core DispatcherServlet handling Comaas healthcheck and other endpoints.
     */
    @Configuration
    @ComponentScan(value = "com.ecg.messagebox", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.ecg.messagebox.resources.*", "com.ecg.messagebox.controllers.*"}))
    @ConditionalOnExpression(V2_AND_UPGRADE_TENANTS)
    public static class MsgBoxServices {
    }

    @Configuration
    @ComponentScan(value = "com.ecg.messagecenter", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.ecg.messagecenter.webapi.*"))
    @ConditionalOnExpression(ONLY_V1_TENANTS)
    public static class MsgCenterServices {
    }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.migration")
    @ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
    public static class MigrationConfiguration {
    }

    /**
     * DispatcherServlet handling only V1 tenants.
     */
    @Bean
    @ConditionalOnExpression(ONLY_V1_TENANTS)
    public SpringContextProvider v1ContextProvider(@Value("${replyts.tenant}") String tenant, ApplicationContext context) {
        String tenantPath = "kjca".equals(tenant) ? "/message-center" : "/ebayk-msgcenter";
        return new SpringContextProvider(tenantPath, WebConfiguration.class, "com.ecg.messagecenter.webapi", context);
    }

    /**
     * DispatcherServlet with old controllers handling V2 tenants plus V1 which are in upgrade mode
     */
    @Bean
    @ConditionalOnExpression(V2_AND_UPGRADE_TENANTS)
    public SpringContextProvider v2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgcenter", WebConfiguration.class, "com.ecg.messagebox.controllers", context);
    }

    /**
     * DispatcherServlet with new (cleaned up) resources handling only V2 tenants
     */
    @Bean
    @ConditionalOnExpression(ONLY_V2_TENANTS)
    public SpringContextProvider newV2ContextProvider(ApplicationContext context) {
        return new SpringContextProvider("/msgbox", WebConfiguration.class, "com.ecg.messagebox.resources", context);
    }
}
