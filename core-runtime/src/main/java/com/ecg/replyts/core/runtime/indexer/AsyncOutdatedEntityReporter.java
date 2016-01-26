package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.indexer.OutdatedEntityReporter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class AsyncOutdatedEntityReporter implements OutdatedEntityReporter {

    private final ConversationRepository conversationRepository;

    private final SearchIndexer searchIndexer;

    private static final Logger LOG = LoggerFactory.getLogger(AsyncOutdatedEntityReporter.class);

    private static final Counter OUTDATED_ENTITIES_OCCURED_COUNTER = TimingReports.newCounter("es-outdated-conversations-repaired");

    @Autowired
    public AsyncOutdatedEntityReporter(ConversationRepository conversationRepository, SearchIndexer searchIndexer) {
        this.conversationRepository = conversationRepository;
        this.searchIndexer = searchIndexer;
    }

    @Override
    public void reportOutdated(Collection<String> conversationIds) {
        List<Conversation> reloadedConversations = newArrayList();
        OUTDATED_ENTITIES_OCCURED_COUNTER.inc(conversationIds.size());
        for (String possiblyOutdatedConversation : conversationIds) {
            try {
                MutableConversation conversation = conversationRepository.getById(possiblyOutdatedConversation);
                reloadedConversations.add(conversation);
            } catch (RuntimeException e) {
                LOG.error("repot outdated skipped " + possiblyOutdatedConversation, e);
            }
        }

        searchIndexer.updateSearchAsync(reloadedConversations);
    }
}
