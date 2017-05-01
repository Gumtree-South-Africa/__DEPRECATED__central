package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakReadRepairConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mdarapour.
 */
public class IndexerReadRepairChunkHandler extends IndexerChunkHandler {
    private static final Timer OVERALL_TIMER = TimingReports.newTimer("index-readrepair-chunk");

    private final RiakReadRepairConversationRepository conversationRepository;
    private final SearchIndexer indexer;

    private static final Logger LOG = LoggerFactory.getLogger(IndexerChunkHandler.class);

    @Autowired
    public IndexerReadRepairChunkHandler(
            ConversationRepository conversationRepository,
            RiakReadRepairConversationRepository readRepairConversationRepository,
            SearchIndexer indexer) {
        super(conversationRepository, indexer, 5000);
        this.conversationRepository = readRepairConversationRepository;
        this.indexer = indexer;
    }

    public void indexChunk(List<String> conversationIds) {
        if (conversationIds.isEmpty()) {
            return;
        }
        Timer.Context timer = OVERALL_TIMER.time();
        try {
            List<Conversation> conversations = new ArrayList<Conversation>();
            for (String convId : conversationIds) {
                try {
                    MutableConversation conversation = conversationRepository.getById(convId);
                    // might be null for very old conversation that have been removed by the cleanup job while the indexer
                    // was running.
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                } catch (Exception e) {
                    LOG.error("Indexer could not load conversation '" + convId + "' from repository - skipping it", e);
                }

            }
            indexer.updateSearchSync(conversations);
        } finally {
            timer.stop();
        }
    }
}