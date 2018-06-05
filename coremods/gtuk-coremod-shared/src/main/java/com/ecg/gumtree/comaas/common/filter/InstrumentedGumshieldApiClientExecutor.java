package com.ecg.gumtree.comaas.common.filter;

import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link org.jboss.resteasy.client.ClientExecutor} that extends Http Client executor. It is
 * responsible for decorating outbound requests with additional data/parameters required for
 * Bushfire API requests.
 */
public final class InstrumentedGumshieldApiClientExecutor extends ApacheHttpClient4Executor {

    private static final Logger LOG = LoggerFactory.getLogger(InstrumentedGumshieldApiClientExecutor.class);

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
        ClientResponse response = super.execute(request);

        int status = response.getStatus();
        String queryParams = request.getQueryParameters().toString();
        String pathParams = request.getPathParameters().toString();
        String method = request.getHttpMethod();
        String uri = request.getUri();

        LOG.info("INSTRUMENTED CALL - METHOD: " + method + ", URI: " + uri + ", " + pathParams + " | " + queryParams + " ||| STATUS: " + status);
        return response;
    }
}