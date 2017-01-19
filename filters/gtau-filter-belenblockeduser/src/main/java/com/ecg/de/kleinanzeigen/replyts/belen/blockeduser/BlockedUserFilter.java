package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * filter that connects to a belen/box style database and queries for the user's state. if the user's state is <code>BLOCKED</code>
 */
public class BlockedUserFilter implements Filter {

    private final CompoundUserStateResolver compoundUserStateResolver;

    public BlockedUserFilter(JdbcTemplate jdbcTemplate) {
        this(new CompoundUserStateResolver(jdbcTemplate));
    }

    public BlockedUserFilter(CompoundUserStateResolver compoundUserStateResolver) {
        this.compoundUserStateResolver = compoundUserStateResolver;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        MessageDirection messageDirection = messageProcessingContext.getMessageDirection();

        String senderMailAddress = messageProcessingContext.getConversation().getUserIdFor(messageDirection.getFromRole());
        UserState state = compoundUserStateResolver.resolve(senderMailAddress);


        if (state == UserState.BLOCKED) {
            return ImmutableList.<FilterFeedback>of(new FilterFeedback(
                    "BLOCKED",
                    "User is blocked " + senderMailAddress,
                    0,
                    FilterResultState.DROPPED));
        }

        return Collections.emptyList();
    }
}
