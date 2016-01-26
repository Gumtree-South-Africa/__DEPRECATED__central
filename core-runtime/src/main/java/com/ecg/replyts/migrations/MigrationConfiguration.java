package com.ecg.replyts.migrations;

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
class MigrationConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean(name = "migrationsApiContext")
    public SpringContextProvider apiContext() {
        return new SpringContextProvider("/migrations", new String[]{"classpath:/migrations/migrations-web-context.xml"}, applicationContext);
    }


}
