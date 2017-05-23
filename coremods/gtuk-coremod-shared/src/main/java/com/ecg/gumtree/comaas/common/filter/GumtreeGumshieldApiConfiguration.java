package com.ecg.gumtree.comaas.common.filter;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.impl.RemoteGumshieldApiFactoryBean;
import com.gumtree.gumshield.api.client.spec.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class GumtreeGumshieldApiConfiguration {
    @Value("${gumshield.api.base_uri:localhost}")
    private String apiBaseUri;

    @Value("${gumshield.api.connection.timeout:10000}")
    private int apiConnectionTimeout;

    @Value("${gumshield.api.socket.timeout:10000}")
    private int apiSocketTimeout;

    @Bean
    public RemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean() {
        RemoteGumshieldApiFactoryBean factoryBean = new RemoteGumshieldApiFactoryBean();
        factoryBean.setBaseUri(apiBaseUri);
        factoryBean.setConnectionTimeout(apiConnectionTimeout);
        factoryBean.setSocketTimeout(apiSocketTimeout);
        return factoryBean;
    }

    @Bean
    public GumshieldApi gumshieldApi(RemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean) throws Exception {
        return remoteGumshieldApiFactoryBean.getObject();
    }

    @Bean
    public UserApi userApi(RemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean) throws Exception {
        return remoteGumshieldApiFactoryBean.getObject().userApi();
    }
}
