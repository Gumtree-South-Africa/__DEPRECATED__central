package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

public class BlockedIpFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(BlockedIpFilter.class);
    private static final String IP_ADDR_HEADER = "X-Cust-Ip";
    private static final List<FilterFeedback> OK_OUTCOME_NO_SCORE = Collections.emptyList();
    private static final String SELECT_EXPIRATION_FOR_IP_WITHIN_RANGES = "SELECT MAX(expiration_date) FROM ip_ranges WHERE ? BETWEEN begin_ip AND end_ip";

    private final JdbcTemplate jdbcTemplate;

    public BlockedIpFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String ip = messageProcessingContext.getMessage().getCaseInsensitiveHeaders().get(IP_ADDR_HEADER);
        if (Strings.isNullOrEmpty(ip)) {
            LOG.trace("IP Header not found");
            return OK_OUTCOME_NO_SCORE;
        }

        Timestamp expirationDate = getIpBlockExpiration(ip);
        if (expirationDate != null && expirationDate.after(new Timestamp(System.currentTimeMillis()))) {
            LOG.trace("IP is blocked. [{}]", ip);
            return ImmutableList.of(new FilterFeedback("BLOCKED", "IP Address is blocked " + ip, 0, FilterResultState.DROPPED));
        } else {
            LOG.trace("IP is not blocked. [{}]", ip);
            return OK_OUTCOME_NO_SCORE;
        }
    }

    private Timestamp getIpBlockExpiration(String ipAddress) {
        try {
            List<Timestamp> resultSet = jdbcTemplate.query(SELECT_EXPIRATION_FOR_IP_WITHIN_RANGES, (rs, rowNum) -> rs.getTimestamp(1), ipAddress);
            if (resultSet == null || resultSet.isEmpty()) {
                return null;
            }
            return resultSet.get(0);
        } catch (DataAccessException dae) {
            // DataAccessException can be thrown by Spring JDBC Template in case of any DB related issues
            reportExternalServiceFailure("mysql_get_blocked_ip");
            LOG.error("Query '{}' with param '{}' failed", SELECT_EXPIRATION_FOR_IP_WITHIN_RANGES, ipAddress, dae);
            return null;
        }
    }
}
