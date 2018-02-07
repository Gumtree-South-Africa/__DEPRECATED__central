package com.ecg;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class PluginConfiguration {
    @Configuration
    @ComponentScan("com.ecg.messagebox")
    @ConditionalOnExpression("#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde' || ('${replyts.tenant}' == 'gtuk' && '${webapi.sync.uk.enabled}' == 'true') || ('${replyts.tenant}' == 'ebayk' && '${webapi.sync.ek.enabled}' == 'true')}")
    static class MessageBoxConfiguration { }

    @Configuration
    @ComponentScan("com.ecg.messagecenter")
    @ConditionalOnExpression("#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}")
    static class MessageCenterConfiguration { }

    @Bean
    public SpringContextProvider messageCenterContextProvider(@Value("${replyts.tenant}") String tenant, ApplicationContext context)  {
        return new SpringContextProvider(getPath(tenant), WebConfiguration.class, context);
    }

    private static String getPath(String tenant) {
        switch (tenant) {
            case "ar":
            case "mx":
            case "sg":
            case "za":
            case "ebayk":
            case "gtau":
            case "gtuk":
            case "it":
                return "/ebayk-msgcenter";
            case "kjca":
                return "/message-center";
            default:
                return "/msgcenter";
        }
    }
}
