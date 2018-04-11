package com.ecg.comaas.gtau.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class BlockedUserFilterFactory implements FilterFactory {

    private static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.belen.blockeduser.BlockedUserFilterFactory";

    private DataSource datasource;

    BlockedUserFilterFactory(DataSource datasource) {
        this.datasource = datasource;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BlockedUserFilter(new JdbcTemplate(datasource));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
