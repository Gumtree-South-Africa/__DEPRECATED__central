package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import com.ecg.replyts.core.runtime.mailparser.HtmlRemover;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Will get picked up by the Application @ComponentScan (com.ecg.replyts.app.*)

@Configuration
public class BoltOverridesConfiguration {
    static {
        HtmlRemover.IS_SPAN_FIX_ENABLED = true;
    }

    @Primary
    @Bean
    public Anonymizer anonymizer(MultiTennantMailCloakingService mailCloakingService) {
        return new BoltAnonymizer(mailCloakingService);
    }
}
