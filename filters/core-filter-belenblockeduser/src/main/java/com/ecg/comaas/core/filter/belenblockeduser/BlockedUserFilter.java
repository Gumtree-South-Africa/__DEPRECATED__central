package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

/**
 * filter that connects to a belen/box style database and queries for the user's state. if the user's state is <code>BLOCKED</code>
 */
class BlockedUserFilter implements Filter {

    private final StateDataSource stateDataSource;

    public BlockedUserFilter(JdbcTemplate jdbcTemplate, boolean extTnsEnabled) {
        this.stateDataSource = new StateDataSource(jdbcTemplate, extTnsEnabled);
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        MessageDirection messageDirection = messageProcessingContext.getMessageDirection();
        String senderMailAddress = messageProcessingContext.getConversation().getUserIdFor(messageDirection.getFromRole());
        UserState state = stateDataSource.getState(senderMailAddress);

        if (UserState.BLOCKED == state) {
            return ImmutableList.of(
                    new FilterFeedback("BLOCKED", "User is blocked " + senderMailAddress, 0, FilterResultState.DROPPED));
        }
        return Collections.emptyList();
    }
}
