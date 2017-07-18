package com.ecg.kijijiit.blockeduser;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * filters the emails when the sender is blacklisted
 * Created by ddallemule on 2/10/14.
 */
public class BlockedUserFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BlockedUserFilter.class);
    private UserStateService userStateService;

    public BlockedUserFilter(UserStateService userStateService) {
        this.userStateService = userStateService;
    }

    @Override public List<FilterFeedback> filter(MessageProcessingContext context) {

        LOG.debug("Applying filter");
        MessageDirection messageDirection = context.getMessageDirection();
        String senderMailAddress =
                        context.getConversation().getUserIdFor(messageDirection.getFromRole());
        boolean blocked = false;
        try {
            blocked = userStateService.isBlocked(senderMailAddress);
        } catch (Exception e) {
            LOG.error("Failed to apply filter senderEmail: " + senderMailAddress + " : " + e
                            .getMessage(), e);
        }

        if (blocked) {
            return ImmutableList.<FilterFeedback>of(new FilterFeedback("BLOCKED",
                            String.format("User [%s] is blocked.", senderMailAddress), 0,
                            FilterResultState.DROPPED));
        }
        return Collections.emptyList();
    }
}
