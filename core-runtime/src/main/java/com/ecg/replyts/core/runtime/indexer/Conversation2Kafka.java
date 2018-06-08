package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Conversation2Kafka {

    private static final Logger LOG = LoggerFactory.getLogger(Conversation2Kafka.class);
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-chunk");

    final AtomicLong fetchedConvCounter = new AtomicLong(0);

    @Value("${search.es.indexname:replyts}")
    private final String indexName = "replyts";

    // This can only be disabled in tests
    @Value("${search.es.enabled:true}")
    private final boolean esEnabled = true;

    @Autowired
    private DocumentSink documentSink;
    @Autowired
    private ConversationRepository conversationRepository;

    public void updateSearchSync(List<Conversation> conversations) {
        if (!esEnabled) {
            return;
        }
        documentSink.sink(conversations);
    }

    public void indexChunk(Set<String> conversationIds) {
        List<Conversation> conversations = fetchConversations(conversationIds);
        documentSink.sink(conversations);
    }

    private List<Conversation> fetchConversations(Set<String> conversationIds) {
        List<Conversation> conversations = new ArrayList<>();

        try (Timer.Context ignore = FETCH_TIMER.time()) {
            for (String conversationId : conversationIds) {
                Conversation conversation = null;
                try {
                    // Might be null for very old conversation that have been removed by the cleanup job while the indexer was running
                    conversation = conversationRepository.getById(conversationId);
                } catch (Exception e) {
                    LOG.warn("Indexer could not load conversation '{}' from repository - skipping it", conversationId, e);
                }
                if (conversation != null) {
                    fetchedConvCounter.incrementAndGet();
                    conversations.add(conversation);
                }
            }

            return conversations;
        }
    }

}