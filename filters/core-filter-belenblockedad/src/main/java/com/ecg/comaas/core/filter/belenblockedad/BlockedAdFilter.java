package com.ecg.comaas.core.filter.belenblockedad;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
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
 * @author maldana@ebay-kleinanzeigen.de
 */
class BlockedAdFilter implements Filter {

    // NO_REASON_AT_ALL is SPAM/FRAUD due to inconsistent data mapping in ebayk tns webapp (we reuse mail-template names as reasons)
    static final String SPAM_FRAUD_REASON = "NO_REASON_AT_ALL";

    private static final List<FilterFeedback> OK_OUTCOME_NO_SCORE = Collections.emptyList();

    private JdbcTemplate jdbcTemplate;

    private final String filtername;

    public BlockedAdFilter(String filtername, JdbcTemplate jdbcTemplate) {
        this.filtername = filtername;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String adId = messageProcessingContext.getConversation().getAdId();
        List<String> resultingStates = jdbcTemplate.query("select delete_reason from ad_tns where ad_id=?", new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        }, adId);

        boolean stateIsBlocked = !resultingStates.isEmpty() && SPAM_FRAUD_REASON.equalsIgnoreCase(resultingStates.get(0));

        if (stateIsBlocked) {
            return ImmutableList.<FilterFeedback>of(new FilterFeedback(
                    "BLOCKED",
                    "Ad is blocked " + adId,
                    0,
                    FilterResultState.DROPPED));
        }

        return OK_OUTCOME_NO_SCORE;
    }
}
