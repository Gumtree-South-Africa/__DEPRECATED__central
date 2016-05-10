package com.ecg.au.gumtree.readrepair;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.core.runtime.indexer.IndexerReadRepairChunkHandler;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakReadRepairConversationRepository;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

@Configuration
class ReadRepairIndexerConfiguration {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private IRiakClient riakClient;
    @Autowired
    private RiakReadRepairConversationRepository readRepairConversationRepository;
    @Autowired
    private RiakConversationRepository conversationRepository;
    @Autowired
    private SearchIndexer searchIndexer;
    @Autowired
    private EmbeddedWebserver webserver;

    @Bean
    public RiakReadRepairConversationRepository riakReadRepairConversationRepository() throws RiakRetryFailedException {
        return new RiakReadRepairConversationRepository(riakClient);
    }

    @Bean
    @Primary
    @Autowired
    public IndexerReadRepairChunkHandler indexerReadRepairChunkHandler() {
        return new IndexerReadRepairChunkHandler(conversationRepository, readRepairConversationRepository, searchIndexer);
    }

    @PostConstruct
    public void context() {
        webserver.context(new SpringContextProvider("/readrepair-indexer", new String[] { "classpath:readrepair-indexer-context.xml" }, context));
    }

}
