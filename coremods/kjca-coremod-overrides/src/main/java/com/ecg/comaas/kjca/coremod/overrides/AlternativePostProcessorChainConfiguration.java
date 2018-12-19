package com.ecg.comaas.kjca.coremod.overrides;

import com.ecg.comaas.kjca.coremod.overrides.postprocessor.Addresser;
import com.ecg.comaas.kjca.coremod.overrides.postprocessor.MessageBodyAnonymizer;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.postprocessorchain.postprocessors.Cleaner;
import com.ecg.replyts.app.postprocessorchain.postprocessors.MessageIdPreparator;
import com.ecg.replyts.core.runtime.mailfixers.BrokenContentTypeFix;
import org.springframework.context.annotation.*;

import java.util.Arrays;

import static com.ecg.replyts.core.api.model.Tenants.*;

// Will get picked up by the Application @ComponentScan (com.ecg.replyts.app.*)

@Configuration
@Profile({TENANT_KJCA, TENANT_MVCA})
@Import({TextAnonymizer.class, BrokenContentTypeFix.class, Addresser.class, MessageBodyAnonymizer.class})
public class AlternativePostProcessorChainConfiguration {
    @Bean
    @Primary
    public PostProcessorChain postProcessorChain(Addresser addresser, Cleaner cleaner, MessageIdPreparator messageIdPreparator, MessageBodyAnonymizer messageBodyAnonymizer) {
        PostProcessorChain chain = new PostProcessorChain();

        chain.setPostProcessors(Arrays.asList(addresser, cleaner, messageIdPreparator, messageBodyAnonymizer));

        return chain;
    }
}