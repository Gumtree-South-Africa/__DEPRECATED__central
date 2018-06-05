package com.ecg.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.impl.RemoteGumshieldApiFactoryBean;
import com.gumtree.gumshield.api.client.spec.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Configuration
public class GumtreeGumshieldApiConfiguration {
    @Value("${gumshield.api.base_uri:localhost}")
    private String apiBaseUri;

    @Value("${gumshield.api.connection.timeout:10000}")
    private int apiConnectionTimeout;

    @Value("${gumshield.api.socket.timeout:10000}")
    private int apiSocketTimeout;

    @Bean
    public InstrumentedRemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean() {
        InstrumentedRemoteGumshieldApiFactoryBean factoryBean = new InstrumentedRemoteGumshieldApiFactoryBean();
        factoryBean.setBaseUri(apiBaseUri);
        factoryBean.setConnectionTimeout(apiConnectionTimeout);
        factoryBean.setSocketTimeout(apiSocketTimeout);
        return factoryBean;
    }

    @Bean
    public GumshieldApi gumshieldApi(InstrumentedRemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean) throws Exception {
        return remoteGumshieldApiFactoryBean.getObject();
    }

    @Bean
    public UserApi userApi(InstrumentedRemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean) throws Exception {
        return remoteGumshieldApiFactoryBean.getObject().userApi();
    }
}
