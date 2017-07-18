package com.ecg.kijijiit.quickreply;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by fmaffioletti on 28/07/14.
 */
@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.columbus.replyts2.quickreply")
public class QuickReplyPostProcessorConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(QuickReplyPostProcessorConfiguration.class);

    @Value("${replyts.quickreply.placeholders}")
    private String placeholders;

    @Bean
    public PostProcessor getPostProcessor() {
        return new QuickReplyPostProcessor(placeholders);
    }

}
