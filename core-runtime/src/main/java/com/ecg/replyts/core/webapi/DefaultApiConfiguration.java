package com.ecg.replyts.core.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 13:17
 *
 * @author maldana@ebay.de
 */
@Configuration
public class DefaultApiConfiguration {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private EmbeddedWebserver webserver;

    // Inject as a boolean so that we can @DependsOn in ReplyTS' EmbeddedWebserver initialization

    @Bean
    @Lazy(false)
    public boolean defaultContextsInitialized() {
        webserver.context(new SpringContextProvider("/screeningv2", new String[] { "classpath:screening-mvc-context.xml" }, context));
        webserver.context(new SpringContextProvider("/configv2", new String[] { "classpath:config-mvc-context.xml" }, context));

        return true;
    }
}