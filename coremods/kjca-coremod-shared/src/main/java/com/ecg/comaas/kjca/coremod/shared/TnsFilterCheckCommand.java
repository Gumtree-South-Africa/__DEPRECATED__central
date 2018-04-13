package com.ecg.comaas.kjca.coremod.shared;

import ca.kijiji.rsc.RemoteServiceCommand;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

class TnsFilterCheckCommand extends RemoteServiceCommand<Map<String, Boolean>, TnsFilterCheckCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(TnsFilterCheckCommand.class);

    private static final int EXECUTION_TIMEOUT_MILLIS = 1025;
    private static final int THREAD_POOL_SIZE = 16;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CIRCUIT_BREAKER_NAME = "tnsfiltercheck";
    private static final String THREAD_POOL_NAME = "TnsFilterCheck";

    TnsFilterCheckCommand(final HttpClient httpClient, final URI endpoint) {
        withHttpClient(httpClient);
        withMaximumCommandTime(EXECUTION_TIMEOUT_MILLIS);
        withMaxThreadsUsed(THREAD_POOL_SIZE);
        withThreadPoolName(THREAD_POOL_NAME);
        withCircuitBreakerName(CIRCUIT_BREAKER_NAME);
        withSpecificEndpoints(Collections.singletonList(endpoint));
        withResponse(OK_200, s -> {
            try {
                return MAPPER.readValue(s, new TypeReference<Map<String, Boolean>>() {
                });
            } catch (IOException e) {
                LOG.warn("Can't convert JSON ({}) to Map!", s);
                return Collections.emptyMap();
            }
        });
    }

    @Override
    protected void onRSCException(Throwable throwable) {
        LOG.warn("Encountered exception while checking filtered-ness at URL: " + this.request.getURI().toString(), throwable);
    }
}
