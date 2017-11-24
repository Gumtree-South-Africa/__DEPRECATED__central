package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IndexingConfiguration {
    @Value("${streaming.indexing.bulk.flush.sizemb:12}")
    private int batchSizeToFlushMb;

    @Value("${streaming.indexing.concurrency:32}")
    private int concurrency;

    @Value("${streaming.indexing.max.actions:10000}")
    private int maxActions;

    @Bean
    public SearchIndexer searchIndexer(Client client, MailCloakingService mailCloakingService) {
        return new SearchIndexer(client, new IndexDataBuilder(mailCloakingService));
    }

    @Bean
    public BulkIndexer bulkIndexer(Client client, MailCloakingService mailCloakingService) {
        return new BulkIndexer(client, new IndexDataBuilder(mailCloakingService), concurrency, batchSizeToFlushMb, maxActions);
    }
}