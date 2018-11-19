package com.ecg.gumtree.comaas.common.filter;

import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistEntry;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import com.gumtree.gumshield.api.domain.known_good.KnownGoodResponse;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.core.MediaType;

public class GumshieldClient {

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

    public ApiChecklistEntry checkByValue(ApiChecklistType type, ApiChecklistAttribute attribute, String value) {
        return target.path(CHECKLIST_ATTRIBUTE_VALUE)
                .resolveTemplate("type", type)
                .resolveTemplate("attribute", attribute)
                .resolveTemplate("value", value)
                .request(MediaType.APPLICATION_JSON)
                .get(ApiChecklistEntry.class);
    }
}
