package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class BlockedUserFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.belen.blockeduser.BlockedUserFilterFactory";

    private DataSource datasource;

    @Autowired
    public BlockedUserFilterFactory(@Qualifier("userDataSource") DataSource datasource) {
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
