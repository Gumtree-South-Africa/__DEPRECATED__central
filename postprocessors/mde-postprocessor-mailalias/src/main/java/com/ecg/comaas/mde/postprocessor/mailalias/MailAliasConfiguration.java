package com.ecg.comaas.mde.postprocessor.mailalias;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class MailAliasConfiguration {

    @Bean
    public MailAliasPostProcessor mailAliasPostProcessor() {
        return new MailAliasPostProcessor();
    }

}
