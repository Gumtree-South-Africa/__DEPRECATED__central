package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.BulkIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class IndexerBulkHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerBulkHandler.class);
    private static final Logger FAILED_IDX = LoggerFactory.getLogger("IndexingFailedConversations");

    private static final Timer INDEX_TIMER = TimingReports.newTimer("index-chunk");
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-chunk");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private BulkIndexer indexer;

    public void indexChunk(Set<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return;
        }

        List<Conversation> conversations = fetchConversations(conversationIds);
        try (Timer.Context ignore = INDEX_TIMER.time()) {
            indexer.updateIndex(conversations);
        }
    }

    public List<Conversation> fetchConversations(Set<String> conversationIds) {
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

    public Conversation fetchConversation(String convId) {
        try {
            // Might be null for very old conversation that have been removed by the cleanup job while the indexer was running
            return conversationRepository.getById(convId);
        } catch (Exception e) {
            LOG.error("Indexer could not load conversation '" + convId + "' from repository - skipping it", e);
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