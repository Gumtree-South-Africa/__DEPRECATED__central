package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

class IndexingConfiguration {

    @Bean
    @Autowired
    SearchIndexer searchIndexer(Client client, MailCloakingService mailCloakingService) {
        IndexDataBuilder indexDataBuilder = new IndexDataBuilder(mailCloakingService);
        return new SearchIndexer(client, indexDataBuilder);
    }
}