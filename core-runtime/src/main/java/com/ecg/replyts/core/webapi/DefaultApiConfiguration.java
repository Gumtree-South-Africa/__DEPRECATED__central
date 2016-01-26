package com.ecg.replyts.core.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 13:17
 *
 * @author maldana@ebay.de
 */
public class DefaultApiConfiguration {

    @Autowired
    private ApplicationContext applicationContext;


    @Bean
    public SpringContextProvider screeningContext() {
        return new SpringContextProvider("/screeningv2", new String[]{"classpath:replyts-screeningv2-context.xml"}, applicationContext);
    }

    @Bean
    public SpringContextProvider filterConfigContext() {
        return new SpringContextProvider("/configv2", new String[]{"classpath:replyts-configv2-context.xml"}, applicationContext);
    }

}
