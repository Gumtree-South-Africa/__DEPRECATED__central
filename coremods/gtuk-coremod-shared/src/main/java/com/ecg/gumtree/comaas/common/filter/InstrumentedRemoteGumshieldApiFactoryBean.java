package com.ecg.gumtree.comaas.common.filter;

import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.impl.ConfigurableConnectionKeepAliveStrategy;
import com.gumtree.gumshield.api.client.impl.RemoteGumshieldApi;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * Factory bean for creating a {@link RemoteGumshieldApi} instance.
 */
public final class InstrumentedRemoteGumshieldApiFactoryBean extends AbstractFactoryBean<GumshieldApi> {

    private String baseUri;

    private int maxConnectionsPerRoute = 50;

    private int connectionTimeout = 1000;

    private int socketTimeout = 2000;

    private long keepAliveDurationMillis = 60000L;

    public void setKeepAliveDurationMillis(long keepAliveDurationMillis) {
        this.keepAliveDurationMillis = keepAliveDurationMillis;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    @Override
    public Class<?> getObjectType() {
        return GumshieldApi.class;
    }

    @Override
    protected GumshieldApi createInstance() throws Exception {
        Assert.hasLength(baseUri);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
        HttpConnectionParams.setSoTimeout(params, socketTimeout);

        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(SchemeRegistryFactory.createDefault(),
                keepAliveDurationMillis, TimeUnit.MILLISECONDS);
        cm.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
        httpClient.setKeepAliveStrategy(new ConfigurableConnectionKeepAliveStrategy(keepAliveDurationMillis));

        InstrumentedGumshieldClientExecutorFactory clientExecutorFactory =
                new InstrumentedGumshieldClientExecutorFactory(httpClient);

        return new RemoteGumshieldApi(baseUri, clientExecutorFactory);
    }
}
