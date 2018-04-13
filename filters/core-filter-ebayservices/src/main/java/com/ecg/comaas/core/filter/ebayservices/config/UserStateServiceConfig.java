package com.ecg.comaas.core.filter.ebayservices.config;

import de.mobile.ebay.service.userprofile.Config;

/**
 * Created by johndavis on 13.10.17.
 */
public class UserStateServiceConfig implements Config {
    private final String endpointUrl;
    private final String oauthUrl;
    private final String clientId;
    private final String clientSecret;

    public UserStateServiceConfig(String endpointUrl, String oauthUrl, String clientId, String clientSecret) {
        this.endpointUrl = endpointUrl;
        this.oauthUrl = oauthUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    public String getOAuthUrl() {
        return oauthUrl;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }
}
