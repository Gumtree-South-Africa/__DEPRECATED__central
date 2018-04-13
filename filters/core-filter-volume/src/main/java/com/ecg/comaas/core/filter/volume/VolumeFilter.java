package com.ecg.comaas.core.filter.volume;

import com.ecg.comaas.core.filter.volume.registry.OccurrenceRegistry;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

class VolumeFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final Comparator<Window> ORDERED_BY_QUOTA_TIME_SPAN =
            Comparator.comparing(Window::getQuota, Quota.TIME_SPAN_COMPARATOR);

    private final Collection<Window> windows;
    private final OccurrenceRegistry occurrenceRegistry;

    VolumeFilter(Collection<Window> windows, OccurrenceRegistry occurrenceRegistry) {
        this.windows = Ordering.from(ORDERED_BY_QUOTA_TIME_SPAN).immutableSortedCopy(windows);
        this.occurrenceRegistry = checkNotNull(occurrenceRegistry, "occurrenceRegistry");
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        String sender = extractSender(context);
        Date now = new Date();
        occurrenceRegistry.register(sender, context.getMessageId(), context.getMessage().getReceivedAt().toDate());
        for (Window window : windows) {
            Quota quota = window.getQuota();
            Date fromTime = Date.from(now.toInstant().minusMillis(quota.getTimeSpanMillis()));
            long mailsInWindow = occurrenceRegistry.count(sender, fromTime);

            LOG.trace("Num of mails in {} {}: {}", quota.getPerTimeValue(), quota.getPerTimeUnit(), mailsInWindow);
            if (mailsInWindow > quota.getAllowance()) {
                return Collections.singletonList(quotaExceededFeedback(mailsInWindow, quota));
            }
        }
        return Collections.emptyList();
    }

    private static FilterFeedback quotaExceededFeedback(long mailsInWindow, Quota quota) {
        return new FilterFeedback(
                quota.uiHint(),
                quota.describeViolation(mailsInWindow),
                quota.getScore(),
                FilterResultState.OK);
    }

    @VisibleForTesting
    String extractSender(MessageProcessingContext context) {
        Message message = context.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        return context.getConversation().getUserId(fromRole);
    }
}
