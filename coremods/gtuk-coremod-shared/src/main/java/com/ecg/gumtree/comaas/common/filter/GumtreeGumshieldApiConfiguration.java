package com.ecg.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.impl.ConfigurableConnectionKeepAliveStrategy;
import com.gumtree.gumshield.api.client.impl.DefaultGumshieldClientExecutorFactory;
import com.gumtree.gumshield.api.client.impl.GumshieldClientExecutorFactory;
import com.gumtree.gumshield.api.client.impl.RemoteGumshieldApiFactoryBean;
import com.gumtree.gumshield.api.client.spec.UserApi;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

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

    private int maxConnectionsPerRoute = 50;

    private int connectionTimeout = 1000;

    private int socketTimeout = 2000;

    private long keepAliveDurationMillis = 60000L;

    @Bean
    public RemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean() {
        RemoteGumshieldApiFactoryBean factoryBean = new RemoteGumshieldApiFactoryBean();
        factoryBean.setBaseUri(apiBaseUri);
        factoryBean.setConnectionTimeout(apiConnectionTimeout);
        factoryBean.setSocketTimeout(apiSocketTimeout);
        return factoryBean;
    }

    @Bean
    public GumshieldApi gumshieldApi() throws Exception {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
        HttpConnectionParams.setSoTimeout(params, socketTimeout);

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(SchemeRegistryFactory.createDefault(),
                keepAliveDurationMillis, TimeUnit.MILLISECONDS);
        cm.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
        httpClient.setKeepAliveStrategy(new ConfigurableConnectionKeepAliveStrategy(keepAliveDurationMillis));

        GumshieldClientExecutorFactory clientExecutorFactory = new DefaultGumshieldClientExecutorFactory(httpClient);

        return new RemoteGumshieldApi(apiBaseUri, clientExecutorFactory);
    }

    @Bean
    public UserApi userApi(RemoteGumshieldApiFactoryBean remoteGumshieldApiFactoryBean) throws Exception {
        return remoteGumshieldApiFactoryBean.getObject().userApi();
    }
}
