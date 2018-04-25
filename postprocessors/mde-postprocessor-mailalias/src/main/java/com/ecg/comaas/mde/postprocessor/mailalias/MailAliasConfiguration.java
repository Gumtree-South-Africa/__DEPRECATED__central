package com.ecg.comaas.mde.postprocessor.mailalias;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class MailAliasConfiguration {

    @Bean
    public MailAliasPostProcessor mailAliasPostProcessor() {
        return new MailAliasPostProcessor();
    }

}
