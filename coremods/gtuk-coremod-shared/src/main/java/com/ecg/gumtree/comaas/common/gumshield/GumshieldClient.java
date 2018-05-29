package com.ecg.gumtree.comaas.common.gumshield;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.BiPredicate;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

public class GumshieldClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GumshieldClient.class);

    private static final BiPredicate<Response, Exception> FAILED_INVOCATION = (response, ex) ->
        ex != null || response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL;

    private final JerseyClient client;
    private final RetryPolicy retryPolicy;
    private final WebTarget target;

    public GumshieldClient(String baseUri, int socketTimeout, int connectionTimeout, int retries) {
        this.retryPolicy = new RetryPolicy()
                .withDelay(1000, TimeUnit.MILLISECONDS)
                .withMaxRetries(retries)
                .retryIf(FAILED_INVOCATION);

        this.client = JerseyClientBuilder.createClient();
        this.client.property(ClientProperties.READ_TIMEOUT, socketTimeout);
        this.client.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
        this.target = client.target(baseUri);
    }

    public boolean existsEntryByValue(ApiChecklistType type, ApiChecklistAttribute attribute, String value) {
        WebTarget target = this.target.path("/checklists/{type}/{attribute}/{value}")
                .resolveTemplate("type", type)
                .resolveTemplate("attribute", attribute)
                .resolveTemplate("value", value);

        Response response = Failsafe.with(retryPolicy).get(() -> target.request().get());

        boolean result = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        if (!result) {
            LOG.warn("Could not find checklist entry: URL: " + target.getUri().toString() + ", Status code: " + response.getStatus());
        }

        return result;
    }

    public boolean knownGood(long id) {
        WebTarget target = this.target.path("/users/{id}/known-good")
                .resolveTemplate("id", id);

        Response response = Failsafe.with(retryPolicy).get(() -> target.request(MediaType.APPLICATION_JSON).get());

        boolean result = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        if (!result) {
            LOG.warn("Could not find knownGood entry: URL: " + target.getUri().toString() + ", Status code: " + response.getStatus());
            return false;
        }

        KnownGoodResponse knownGoodResponse = response.readEntity(KnownGoodResponse.class);
        return knownGoodResponse.getStatus() == KnownGoodStatus.GOOD;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
