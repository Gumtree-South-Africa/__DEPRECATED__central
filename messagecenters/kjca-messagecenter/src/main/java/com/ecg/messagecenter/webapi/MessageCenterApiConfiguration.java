package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import org.springframework.context.annotation.Configuration;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:00
 *
 * @author maldana@ebay.de
 */
@Configuration
class MessageCenterApiConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EmbeddedWebserver webserver;

    @PostConstruct
    public void context() {
        webserver.context(new SpringContextProvider("/message-center", new String[]{"classpath:msgcenter-context.xml"}, applicationContext));
    }
}
