package com.ecg.mde.filter.badword;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class BadwordConfiguration {

    @Value("${replyts.mobile.badword.csFilterServiceEndpoint}")
    private String csEndpoint;

    @Bean
    public BadwordFilterFactory badwordFilterFactory(CsFilterServiceClient client) {
        return new BadwordFilterFactory(client);
    }

    @Bean
    public CsFilterServiceClient csFilterServiceClient() {
        return new CsFilterServiceClient(csEndpoint);
    }
}
