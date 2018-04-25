package com.ecg.comaas.synchronizer;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
@ComponentScan
@EnableConfigurationProperties
public class MessageSynchronizerListenerConfiguration {

}
