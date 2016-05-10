package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @author mdarapour
 */
class BlockedIpFilterFactory implements FilterFactory {
    private DataSource datasource;

    public BlockedIpFilterFactory(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BlockedIpFilter(s, new JdbcTemplate(datasource));
    }
}
