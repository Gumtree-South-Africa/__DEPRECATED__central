package com.ecg;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@ComaasPlugin
@Configuration("core-messagecenter")
public class PluginConfiguration {
    @Configuration
    @ComponentScan("com.ecg.messagebox")
    @ConditionalOnExpression("#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde'}")
    static class MessageBoxConfiguration { }

    @Configuration
    @ComponentScan("com.ecg.messagecenter")
    @ConditionalOnExpression("#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}")
    static class MessageCenterConfiguration { }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EmbeddedWebserver webserver;

    @Value("${replyts.tenant}")
    private String tenant;

    @PostConstruct
    public void context()  {
        webserver.context(new SpringContextProvider(getPath(tenant), WebConfiguration.class, context));
    }

    private static String getPath(String tenant) {
        switch (tenant) {
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
