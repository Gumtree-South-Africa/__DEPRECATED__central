package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class BlockedIpFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BlockedIpFilter.class);

    static final String IP_ADDR_HEADER = "X-Cust-Ip";

    private static final List<FilterFeedback> OK_OUTCOME_NO_SCORE = Collections.emptyList();

    private JdbcTemplate jdbcTemplate;

    BlockedIpFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String ip = messageProcessingContext.getMessage().getHeaders().get(IP_ADDR_HEADER);

        if (Strings.isNullOrEmpty(ip)) {
            LOG.trace("IP Header not found");
            return OK_OUTCOME_NO_SCORE;
        }

        List<Timestamp> expirationDates = jdbcTemplate.query("select expiration_date from ip_ranges where ? between begin_ip and end_ip",
                (rs, rowNum) -> rs.getTimestamp(1), ip);

        Collections.sort(expirationDates);
        boolean ipIsBlocked = !expirationDates.isEmpty() && expirationDates.get(expirationDates.size() - 1).after(new Timestamp(System.currentTimeMillis()));

        if (ipIsBlocked) {
            LOG.trace("IP is blocked. [{}]", ip);
            return ImmutableList.of(new FilterFeedback(
                    "BLOCKED",
                    "IP Address is blocked " + ip,
                    0,
                    FilterResultState.DROPPED));
        }

        LOG.trace("IP is not blocked. [{}]", ip);
        return OK_OUTCOME_NO_SCORE;
    }
}
