package com.ecg.replyts.core.webapi.screeningv2;


import com.ecg.replyts.core.api.indexer.OutdatedEntityReporter;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class OutdatedEntityMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(OutdatedEntityMonitor.class);

    private final OutdatedEntityReporter entityReporter;

    @Autowired
    OutdatedEntityMonitor(OutdatedEntityReporter entityReporter) {
        this.entityReporter = entityReporter;
    }

    void scan(List<MessageRts> messages, MessageRtsState expectedState) {
        Preconditions.checkArgument(canOutdate(expectedState));
        List<String> outdatedConversationIds = Lists.newArrayList();
        for (MessageRts message : messages) {
            if (message.getState() != expectedState) {
                outdatedConversationIds.add(message.getConversation().getId());
                LOG.warn("Conv/Message {}/{} is outdated. State in Index: {}, Actual state: {}. Reindexing message.",
                        message.getConversation().getId(), message.getId(), expectedState, message.getState());
            }
        }

        if (!outdatedConversationIds.isEmpty()) {
            entityReporter.reportOutdated(outdatedConversationIds);
        }

    }

    boolean canOutdate(MessageRtsState expectedState) {
        return expectedState == MessageRtsState.HELD || expectedState == MessageRtsState.SENT || expectedState == MessageRtsState.BLOCKED;
    }
}
