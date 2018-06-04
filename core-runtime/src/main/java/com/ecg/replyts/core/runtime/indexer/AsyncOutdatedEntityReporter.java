package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.indexer.OutdatedEntityReporter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class AsyncOutdatedEntityReporter implements OutdatedEntityReporter {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncOutdatedEntityReporter.class);

    private static final Counter OUTDATED_ENTITIES_OCCURED_COUNTER = TimingReports.newCounter("es-outdated-conversations-repaired");

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private Conversation2Kafka conversation2Kafka;

    @Override
    public void reportOutdated(Collection<String> conversationIds) {
        List<Conversation> reloadedConversations = new ArrayList<>();

        OUTDATED_ENTITIES_OCCURED_COUNTER.inc(conversationIds.size());

        for (String possiblyOutdatedConversation : conversationIds) {
            try {
                reloadedConversations.add(conversationRepository.getById(possiblyOutdatedConversation));
            } catch (RuntimeException e) {
                LOG.error("Report outdated skipped " + possiblyOutdatedConversation, e);
            }
        }

        conversation2Kafka.updateSearchSync(reloadedConversations);
    }
}