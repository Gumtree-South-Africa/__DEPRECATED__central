package com.ecg.comaas.mde.postprocessor.addmetadata;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class AddMetadataConfiguration {

    @Value("${replyts.mobilede.addmetadata.plugin.order}")
    private int order;

    @Bean
    public AddMetadataPostProcessor mailAddMetadataPostProcessor() {
        return new AddMetadataPostProcessor(order);
    }
}
