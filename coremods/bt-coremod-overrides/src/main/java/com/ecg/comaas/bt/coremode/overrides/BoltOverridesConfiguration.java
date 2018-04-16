package com.ecg.comaas.bt.coremode.overrides;

import com.ecg.replyts.app.postprocessorchain.postprocessors.Anonymizer;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTenantMailCloakingService;
import com.ecg.replyts.core.runtime.mailparser.HtmlRemover;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Will get picked up by the Application @ComponentScan (com.ecg.replyts.app.*)

@ComaasPlugin
@Configuration
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
