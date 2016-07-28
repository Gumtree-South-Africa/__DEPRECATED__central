package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
class MessageCenterApiConfiguration {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EmbeddedWebserver webserver;

    @PostConstruct
    public void context() {
        webserver.context(new SpringContextProvider("/msgcenter", new String[]{"classpath:msgcenter-context.xml"}, context));
    }
}