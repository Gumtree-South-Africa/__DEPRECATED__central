package com.ecg.de.ebayk.messagecenter.webapi;

import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:00
 *
 * @author maldana@ebay.de
 */
class MessageCenterApiConfiguration {

    @Autowired private ApplicationContext applicationContext;

    @Bean public SpringContextProvider apiContext() {
        return new SpringContextProvider("/ebayk-msgcenter",
                        new String[] {"classpath:ebayk-msgcenter-context.xml"}, applicationContext);
    }


}
