package com.ecg.comaas.mde.postprocessor.donotanonymize;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class DoNotAnonymizeConfiguration {

    @Value("${replyts.doNotAnonymize.password:J2y$idmEqLo2yyDyope}")
    private String password;

    @Bean
    public DoNotAnonymizePostProcessor doNotAnonymizePostProcessor() {
        return new DoNotAnonymizePostProcessor(password);
    }
}
