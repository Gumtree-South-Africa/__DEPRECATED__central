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
import java.util.Map;

class VolumeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final String ESPER_ALREADY_NOTIFIED = "ESPER_ALREADY_NOTIFIED";

    private final SharedBrain sharedBrain;
    private final List<Window> windows;
    private final EventStreamProcessor processor;

    VolumeFilter(SharedBrain sharedBrain, EventStreamProcessor processor, List<Window> windows) {
        this.sharedBrain = sharedBrain;
        this.processor = processor;
        this.windows = windows;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Message message = context.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMail = context.getConversation().getUserId(fromRole);

        // Only the first VolumeFilter should notify the other Esper/Hazelcast nodes.
        Map<String, Object> filterContext = context.getFilterContext();
        if (!filterContext.containsKey(ESPER_ALREADY_NOTIFIED)) {
            LOG.debug("Marking a messages '{}' sent by user '{}' as seen and publish to Hazelcast.", message.getId(), senderMail);
            sharedBrain.markSeen(senderMail);
            filterContext.put(ESPER_ALREADY_NOTIFIED, Boolean.TRUE);
        }

        for (Window window : windows) {
            long mailsInWindow = processor.count(senderMail, window);
            Quota quota = window.getQuota();

            LOG.debug("Num of mails in {} {} for [{}]: {}", quota.getPerTimeValue(), quota.getPerTimeUnit(), senderMail, mailsInWindow);

            if (mailsInWindow > quota.getAllowance()) {
                return Collections.singletonList(new FilterFeedback(
                        quota.uihint(),
                        quota.describeViolation(mailsInWindow),
                        quota.getScore(),
                        FilterResultState.OK));
            }
        }

        return Collections.emptyList();
    }
}
