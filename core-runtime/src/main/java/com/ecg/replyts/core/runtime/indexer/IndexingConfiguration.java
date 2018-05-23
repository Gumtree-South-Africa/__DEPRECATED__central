package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.MailCloakingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexingConfiguration {

    @Bean
    public IndexDataBuilder indexDataBuilder(MailCloakingService mailCloakingService) {
        return new IndexDataBuilder(mailCloakingService);
    }

}