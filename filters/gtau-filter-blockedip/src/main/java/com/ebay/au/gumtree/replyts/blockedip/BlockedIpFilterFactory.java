package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BlockedIpFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ebay.au.gumtree.replyts.blockedip.BlockedIpFilterFactory";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BlockedIpFilterFactory(@Qualifier("blockedIpJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BlockedIpFilter(jdbcTemplate);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
