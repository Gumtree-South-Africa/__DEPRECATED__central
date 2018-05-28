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

    private static final BiPredicate<Response, Exception> FAILED_INVOCATION = (response, ex) -> {
        if (ex != null) {
            LOG.warn("GumshieldClient invocation failed with an exception.", ex.getMessage());
            return true;
        }

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            LOG.warn("GumshieldClient invocation failed with a wrong status code: " + response.getStatus());
            return true;
        }

        return false;
    };

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
        Response response = Failsafe.with(retryPolicy).get(() ->
                target.path("/checklists/{type}/{attribute}/{value}")
                        .resolveTemplate("type", type)
                        .resolveTemplate("attribute", attribute)
                        .resolveTemplate("value", value)
                        .request()
                        .get());

        boolean result = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        if (!result) {
            LOG.info("Could not find checklist entry for " + attribute + ", Status code: " + response.getStatus());
        }

        return result;
    }

    public boolean knownGood(long id) {
        Response response = Failsafe.with(retryPolicy).get(() ->
                target.path("/users/{id}/known-good")
                        .resolveTemplate("id", id)
                        .request(MediaType.APPLICATION_JSON)
                        .get());

        boolean result = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        if (!result) {
            LOG.info("Could not find knownGood entry for '" + id + "', Status code: " + response.getStatus());
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
