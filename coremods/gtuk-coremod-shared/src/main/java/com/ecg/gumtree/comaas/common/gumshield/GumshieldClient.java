package com.ecg.gumtree.comaas.common.gumshield;

import com.google.gson.JsonObject;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.PathParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class GumshieldClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GumshieldClient.class);

    private final JerseyClient client;
    private final WebTarget target;

    public GumshieldClient(String baseUri) {
        this.client = JerseyClientBuilder.createClient();
        this.client.property(ClientProperties.CONNECT_TIMEOUT, 2000);
        this.client.property(ClientProperties.READ_TIMEOUT, 2000);
        this.target = client.target(baseUri);
    }

    public boolean existsEntryByValue(ApiChecklistType type, ApiChecklistAttribute attribute, String value) {
        Response response = target.path("/checklists/{type}/{attribute}/{value}")
                .resolveTemplate("type", type)
                .resolveTemplate("attribute", attribute)
                .resolveTemplate("value", value)
                .request()
                .get();

        boolean result = response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
        if (!result) {
            LOG.info("Could not find checklist entry for " + attribute + ", Status code: " + response.getStatus());
        }

        return result;
    }

    public boolean knownGood(long id) {
        Response response = target.path("/users/{id}/known-good")
                .resolveTemplate("id", id)
                .request(MediaType.APPLICATION_JSON)
                .get();

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
