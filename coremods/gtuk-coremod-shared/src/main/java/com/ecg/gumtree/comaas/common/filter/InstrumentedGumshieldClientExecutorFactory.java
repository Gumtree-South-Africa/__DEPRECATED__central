package com.ecg.gumtree.comaas.common.filter;

import com.gumtree.gumshield.api.client.impl.GumshieldClientExecutorFactory;
import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.ClientExecutor;

public final class InstrumentedGumshieldClientExecutorFactory implements GumshieldClientExecutorFactory {

    private HttpClient httpClient;

    /**
     * Constructor.
     *
     * @param httpClient          the http client for executing requests
     */
    public InstrumentedGumshieldClientExecutorFactory(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public ClientExecutor create() {
        return new InstrumentedGumshieldApiClientExecutor(httpClient);
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }
}
