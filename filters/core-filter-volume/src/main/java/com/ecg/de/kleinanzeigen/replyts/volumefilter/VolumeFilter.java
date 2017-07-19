package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

class VolumeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private final SharedBrain sharedBrain;
    private final List<Quota> sortedQuotas;
    private final EventStreamProcessor processor;
    private final String instanceId;

    VolumeFilter(SharedBrain sharedBrain, List<Quota> sortedQuotas, EventStreamProcessor processor, String instanceId) {
        this.sharedBrain = sharedBrain;
        this.sortedQuotas = sortedQuotas;
        this.processor = processor;
        this.instanceId = instanceId;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);

        sharedBrain.markSeen(senderMailAddress);

        for (Quota quota : sortedQuotas) {
            long mailsInTimeWindow = processor.count(senderMailAddress, instanceId, quota);

            LOG.debug("Num of mails in {} {}: {}", quota.getPerTimeValue(), quota.getPerTimeUnit(), mailsInTimeWindow);

            if (mailsInTimeWindow > quota.getAllowance()) {
                return Collections.singletonList(new FilterFeedback(
                        quota.uihint(),
                        quota.describeViolation(mailsInTimeWindow),
                        quota.getScore(),
                        FilterResultState.OK));
            }
        }

        return Collections.emptyList();
    }
}
