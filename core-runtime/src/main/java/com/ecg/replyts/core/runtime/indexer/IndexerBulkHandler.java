package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.BulkIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class IndexerBulkHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerBulkHandler.class);
    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    private static final Timer INDEX_TIMER = TimingReports.newTimer("index-chunk");
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-chunk");
    private static final Timer SAVE2KAFKA_TIMER = TimingReports.newTimer("save-chunk2kafka");

    @Autowired
    private Document2KafkaSink  document2KafkaSink;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private BulkIndexer indexer;

    @Value("#{'${indexing.2kafka.enabled:false}' == '${region:ams1}' }")
    private Boolean enableReindex2Kafka;

    @PostConstruct
    private void reportConfiguration() {
        if(enableReindex2Kafka) {
            LOG.info("Comaas indexing is disabled, sending documents to kafka for reindex instead");
        } else {
            LOG.info("Indexing with comaas indexer");
        }
    }

    public void indexChunk(Set<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return;
        }
        List<Conversation> conversations = fetchConversations(conversationIds);
        if(enableReindex2Kafka) {
            sendConversations2Kafka(conversations);
        } else {
            try (Timer.Context ignore = INDEX_TIMER.time()) {
                indexer.updateIndex(conversations);
            }
        }
    }

    private void sendConversations2Kafka(List<Conversation> conversations) {
        try (Timer.Context ignore = SAVE2KAFKA_TIMER.time()) {
            for (Conversation conversation : conversations) {
                if (conversation != null) {
                    document2KafkaSink.pushToKafka(conversation);
                }
            }
        }
    }

    private List<Conversation> fetchConversations(Set<String> conversationIds) {
        List<Conversation> conversations = new ArrayList<>();

        try (Timer.Context ignore = FETCH_TIMER.time()) {
            for (String conversationId : conversationIds) {
                Conversation conversation = fetchConversation(conversationId);

                if (conversation != null) {
                    conversations.add(conversation);
                }
            }

            return conversations;
        }
    }

    private Conversation fetchConversation(String convId) {
        try {
            // Might be null for very old conversation that have been removed by the cleanup job while the indexer was running
            return conversationRepository.getById(convId);
        } catch (Exception e) {
            LOG.warn("Indexer could not load conversation '{}' from repository - skipping it", convId, e);
            FAILED_IDX.info(convId);

            return null;
        }
    }

    public void flush() {
        indexer.flush();
    }

    public void resetCounters() {
        indexer.resetCounters();
    }
}