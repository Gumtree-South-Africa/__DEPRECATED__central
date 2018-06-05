package com.ecg.gumtree.comaas.common.filter;

import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

/**
 * Custom {@link org.jboss.resteasy.client.ClientExecutor} that extends Http Client executor. It is
 * responsible for decorating outbound requests with additional data/parameters required for
 * Bushfire API requests.
 */
public final class InstrumentedGumshieldApiClientExecutor extends ApacheHttpClient4Executor {

    /**
     * Constructor.
     *
     * @param httpClient          the {@link HttpClient} to use
     */
    public InstrumentedGumshieldApiClientExecutor(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public ClientResponse execute(ClientRequest request) throws Exception {
        return super.execute(request);
    }
}