package com.ecg.gumtree.comaas.common.filter;

import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodResponse;
import org.glassfish.jersey.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class GumshieldClient {

    private static final Logger LOG = LoggerFactory.getLogger(GumshieldClient.class);

    private static final ClientConfig CLIENT_CONFIG = new ClientConfig()
            .property(ClientProperties.CONNECT_TIMEOUT, 2000)
            .property(ClientProperties.READ_TIMEOUT, 2000);

    private static final String CHECKLIST_ATTRIBUTE_VALUE = "/checklists/{type}/{attribute}/{value}";
    private static final String USER_KNOWN_GOOD = "/users/{id}/known-good";

    private JerseyWebTarget target;

    public GumshieldClient(String baseUri) {
        this.target = JerseyClientBuilder.createClient(CLIENT_CONFIG)
                .target(baseUri);
    }

    public KnownGoodResponse knownGood(long senderId) {
        return target.path(USER_KNOWN_GOOD)
                .resolveTemplate("id ", senderId)
                .request(MediaType.APPLICATION_JSON)
                .get(KnownGoodResponse.class);
    }

    public boolean checkContainsRecord(ApiChecklistType type, ApiChecklistAttribute attribute, String value) {
        JerseyInvocation.Builder request = target.path(CHECKLIST_ATTRIBUTE_VALUE)
                .resolveTemplate("type", type)
                .resolveTemplate("attribute", attribute)
                .resolveTemplate("value", value)
                .request(MediaType.APPLICATION_JSON);
        try {
            Response response = request.get();
            if (response.getStatus() == 200) {
                return true;
            }
            if (response.getStatus() == 404) {
                return false;
            }

            LOG.error("Encountered unexpected response ({}) when sending request containing [{}, {}, {}] to Gumshield: {}", response.getStatus(), type, attribute, value, response);
            return false;
        } catch (Exception e) {
            LOG.error("Encountered error when sending sending request containing [{}, {}, {}] to Gumshield", type, attribute, value, e);
            return false;
        }
    }
}