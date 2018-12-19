package com.ecg.comaas.bt.coremode.overrides;

import com.ecg.replyts.app.postprocessorchain.postprocessors.Anonymizer;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTenantMailCloakingService;
import com.ecg.replyts.core.runtime.mailparser.HtmlRemover;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.*;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_SG;

@ComaasPlugin
@Configuration
@Profile({TENANT_MX, TENANT_AR, TENANT_ZA, TENANT_SG})
public class BoltOverridesConfiguration {
    static {
        HtmlRemover.IS_BOLT_SPAN_FIX_ENABLED = true;
    }

    @Primary
    @Bean
    public Anonymizer anonymizer(MultiTenantMailCloakingService mailCloakingService) {
        return new BoltAnonymizer(mailCloakingService);
    }
}
