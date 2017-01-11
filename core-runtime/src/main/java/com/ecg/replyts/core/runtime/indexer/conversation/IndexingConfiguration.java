package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

class IndexingConfiguration {

    @Bean
    @Autowired
    SearchIndexer searchIndexer(Client client, MailCloakingService mailCloakingService) {
        IndexDataBuilder indexDataBuilder = new IndexDataBuilder(mailCloakingService);
        return new SearchIndexer(client, indexDataBuilder);
    }

    @Value("${streaming.indexing.bulk.flush.sizemb:12}")
    private int batchSizeToFlushMb;

    @Value("${streaming.indexing.concurrency:32}")
    private int concurrency;

    @Value("${streaming.indexing.max.actions:10000}")
    private int maxActions;

    @Bean
    @Autowired
    BulkIndexer bulkIndexer(Client client, MailCloakingService mailCloakingService) {
        IndexDataBuilder indexDataBuilder = new IndexDataBuilder(mailCloakingService);
        return new BulkIndexer(client, indexDataBuilder, concurrency, batchSizeToFlushMb, maxActions);
    }
}