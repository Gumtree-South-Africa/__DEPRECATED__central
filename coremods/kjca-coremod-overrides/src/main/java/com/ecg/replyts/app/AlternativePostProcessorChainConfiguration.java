package com.ecg.replyts.app;

import ca.kijiji.replyts.TextAnonymizer;
import ca.kijiji.replyts.logging.ExceptionMetricsLogger;
import ca.kijiji.replyts.postprocessor.Addresser;
import ca.kijiji.replyts.postprocessor.MessageBodyAnonymizer;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.postprocessorchain.postprocessors.Cleaner;
import com.ecg.replyts.app.postprocessorchain.postprocessors.MessageIdPreparator;
import com.ecg.replyts.core.runtime.mailfixers.BrokenContentTypeFix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

// Will get picked up by the Application @ComponentScan (com.ecg.replyts.app.*)

@Configuration
@Import({ TextAnonymizer.class, ExceptionMetricsLogger.class, BrokenContentTypeFix.class, Addresser.class, MessageBodyAnonymizer.class })
public class AlternativePostProcessorChainConfiguration {
    @Bean
    @Primary
    public PostProcessorChain postProcessorChain(Addresser addresser, Cleaner cleaner, MessageIdPreparator messageIdPreparator, MessageBodyAnonymizer messageBodyAnonymizer) {
        PostProcessorChain chain = new PostProcessorChain();

        chain.setPostProcessors(Arrays.asList(addresser, cleaner, messageIdPreparator, messageBodyAnonymizer));

        return chain;
    }
}