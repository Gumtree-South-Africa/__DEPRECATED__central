package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Conversation2Kafka {

    private static final Logger LOG = LoggerFactory.getLogger(Conversation2Kafka.class);
    private static final Timer FETCH_TIMER = TimingReports.newTimer("fetch-conversation");
    final AtomicLong fetchedConvCounter = new AtomicLong(0);

    @Autowired
    private DocumentSink documentSink;
    @Autowired
    private ConversationRepository conversationRepository;

    public void updateElasticSearch(String conversationId) {
        Conversation conversation = fetchConversation(conversationId);
        if(conversation!=null) {
            documentSink.sink(conversation);
        }
    }

    private Conversation fetchConversation(String conversationId) {
        try (Timer.Context ignore = FETCH_TIMER.time()) {
                  Conversation conversation = null;
                try {
                    // Might be null for very old conversation that have been removed by the cleanup job while the indexer was running
                    conversation = conversationRepository.getById(conversationId);
                } catch (Exception e) {
                    LOG.warn("Indexer could not load conversation '{}' from repository - skipping it", conversationId, e);
                }
                if (conversation != null) {
                    fetchedConvCounter.incrementAndGet();
                }
            return conversation;
        }
    }

}