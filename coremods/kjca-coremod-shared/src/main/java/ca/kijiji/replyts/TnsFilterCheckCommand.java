package ca.kijiji.replyts;

import ca.kijiji.rsc.RemoteServiceCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static org.eclipse.jetty.http.HttpStatus.OK_200;

class TnsFilterCheckCommand extends RemoteServiceCommand<Map, TnsFilterCheckCommand> {
    private static final Logger LOG = LoggerFactory.getLogger(TnsFilterCheckCommand.class);

    private static final int EXECUTION_TIMEOUT_MILLIS = 1025;
    private static final int THREAD_POOL_SIZE = 16;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CIRCUIT_BREAKER_NAME = "tnsfiltercheck";
    private static final String THREAD_POOL_NAME = "TnsFilterCheck";

    TnsFilterCheckCommand(final HttpClient httpClient, final ImmutableList<URI> endpoints) {
        withHttpClient(httpClient);
        withMaximumCommandTime(EXECUTION_TIMEOUT_MILLIS);
        withMaxThreadsUsed(THREAD_POOL_SIZE);
        withThreadPoolName(THREAD_POOL_NAME);
        withCircuitBreakerName(CIRCUIT_BREAKER_NAME);
        withSpecificEndpoints(endpoints);
        withResponse(OK_200, s -> {
            try {
                return MAPPER.readValue(s, Map.class);
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