package com.ecg.comaas.synchronizer;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
@ComponentScan
@EnableConfigurationProperties
public class MessageSynchronizerListenerConfiguration {

}
