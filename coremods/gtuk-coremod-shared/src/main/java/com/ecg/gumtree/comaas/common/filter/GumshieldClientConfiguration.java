package com.ecg.gumtree.comaas.common.filter;

import com.ecg.gumtree.comaas.common.gumshield.GumshieldClient;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Configuration
public class GumshieldClientConfiguration {

    @Bean
    public GumshieldClient gumshieldClient(@Value("${gumshield.api.base_uri:localhost}") String apiBaseUri) {
        return new GumshieldClient(apiBaseUri);
    }
}
