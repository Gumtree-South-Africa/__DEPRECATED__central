package com.ecg.kijijiit.conversationmonitor;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jaludden on 08/05/17.
 */
@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.columbus.replyts2.conversationmonitor")
public class ConversationMoniitorPostProcessorConfiguration {

    @Value("${replyts.conversation.monitor.replaced.chars}")
    private String replacedChars;

    @Bean
    public PostProcessor getPostProcessor() {
        return new ConversationMonitorFilterPostProcessor(replacedChars);
    }
}
